package org.test.message.timewheel;

public interface ExpirationListener<E> {

    void expired(E expiredObject);
}
