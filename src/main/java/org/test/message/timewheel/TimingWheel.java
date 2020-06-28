package org.test.message.timewheel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 定时轮
 * <br/>
 * 能够用一个线程管理多个带超时的对象
 */
public class TimingWheel<E> {
    private final long tickDuration;
    private final int ticksPerWheel;
    private volatile int currentTickIndex = 0;
    private final ArrayList<Slot<E>> wheel;
    private final Map<E, Slot<E>> indicator = new ConcurrentHashMap<>();

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Thread workerThread;
    private final ExecutorService expirationExecutor;
    private ExpirationListener<E>[] expirationListeners;
    private final String name;

    public TimingWheel(String name, int tickDuration, int ticksPerWheel, TimeUnit timeUnit, ExpirationListener<E>... expirationListeners) {
        Objects.requireNonNull(timeUnit, "timeUnit");

        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }

        this.wheel = new ArrayList<>();
        this.tickDuration = TimeUnit.MILLISECONDS.convert(tickDuration, timeUnit);
        this.ticksPerWheel = ticksPerWheel + 1;

        for (int i = 0; i < this.ticksPerWheel; i++) {
            wheel.add(new Slot<>(i));
        }
        wheel.trimToSize();

        if (StringUtils.isBlank(name)) {
            name = "Timing-Wheel";
        }
        workerThread = new Thread(new TickWorker(), name);
        this.name = name;

        this.expirationExecutor = Executors.newCachedThreadPool();
        if (expirationListeners != null && expirationListeners.length > 0) {
            this.expirationListeners = expirationListeners;
        }
    }

    public void addExpireListener(ExpirationListener<E> expirationListener) {
        synchronized (TimingWheel.class) {
            this.expirationListeners = ArrayUtils.add(this.expirationListeners, expirationListener);
        }
    }

    public void start() {
        if (shutdown.get()) {
            throw new IllegalStateException("Cannot be started once stopped");
        }

        if (!workerThread.isAlive()) {
            workerThread.start();
        }
    }

    public boolean stop(boolean expire) {
        Set<E> objs = indicator.keySet();
        for (E e : objs) {
            indicator.remove(e);
            if (expire && expirationListeners != null) {
                for (ExpirationListener<E> listener : expirationListeners) {
                    expirationExecutor.execute(() -> listener.expired(e));
                }
            }
        }
        if (!shutdown.compareAndSet(false, true)) {
            return false;
        }
        boolean interrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        expirationExecutor.shutdownNow();
        return true;
    }

    /**
     * Add a element to {@link TimingWheel} and start to count down its life-time.
     *
     * @param e element
     * @return remain time to be expired in millisecond.
     */
    public long add(E e) {
        if (e == null) {
            return -1;
        }
        synchronized (e) {
            checkAdd(e);

            int previousTickIndex = getPreviousTickIndex();
            Slot<E> slot = wheel.get(previousTickIndex);
            slot.add(e);
            indicator.put(e, slot);

            return (ticksPerWheel - 1) * tickDuration;
        }
    }

    private void checkAdd(E e) {
        if (e == null) {
            return;
        }
        Slot<E> slot = indicator.get(e);
        if (slot != null) {
            slot.remove(e);
        }
    }

    public boolean exists(E e) {
        if (e == null) {
            return false;
        }
        Slot<E> slot = indicator.get(e);
        return slot != null && slot.elements.contains(e);
    }

    private int getPreviousTickIndex() {
        lock.readLock().lock();
        try {
            int cti = currentTickIndex;
            if (cti == 0) {
                return ticksPerWheel - 1;
            }

            return cti - 1;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes the specified element from timing wheel.
     *
     * @param e special element
     * @return <tt>true</tt> if this timing wheel contained the specified
     */
    public boolean remove(E e) {
        if (e == null) {
            return false;
        }
        synchronized (e) {
            Slot<E> slot = indicator.get(e);
            if (slot == null) {
                return false;
            }
            indicator.remove(e);
            return slot.remove(e);
        }
    }

    private void notifyExpired(int idx) {
        Slot<E> slot = wheel.get(idx);
        final Set<E> elements = slot.elements;
        for (final E e : elements) {
            if (e == null) {
                continue;
            }

            slot.remove(e);
            synchronized (e) {
                Slot<E> latestSlot = indicator.get(e);
                if (latestSlot.equals(slot)) {
                    indicator.remove(e);
                }
            }
            if (this.expirationListeners != null && this.expirationListeners.length > 0) {
                for (final ExpirationListener<E> listener : expirationListeners) {
                    expirationExecutor.execute(() -> listener.expired(e));
                }
            }
        }
    }

    private class TickWorker implements Runnable {
        private long startTime;
        private long tick;

        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            tick = 1;
            for (int i = 0; !shutdown.get(); i++) {
                if (i == wheel.size()) {
                    // wheel 列表循环
                    i = 0;
                }
                lock.writeLock().lock();
                try {
                    currentTickIndex = i;
                } finally {
                    lock.writeLock().unlock();
                }

                waitForNextTick();
            }
        }

        private void waitForNextTick() {
            for (; ; ) {
                // 计算得到的睡眠的时间会<=tickDuration，这样计算更准确，考虑了代码中其他部分的执行时间
                long sleepTime = tickDuration * tick - (System.currentTimeMillis() - startTime);
                if (sleepTime <= 0) {
                    break;
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    return;
                }
            }

            tick++;
        }
    }

    private static class Slot<E> {
        private int id;
        private final Set<E> elements = Collections.newSetFromMap(new ConcurrentHashMap<>());

        public Slot(int id) {
            this.id = id;
        }

        public void add(E e) {
            elements.add(e);
        }

        public Boolean remove(E e) {
            return elements.remove(e);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return id == ((Slot<?>) o).id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Slot{" +
                    "id=" + id +
                    ", elements=" + elements +
                    '}';
        }
    }
}
