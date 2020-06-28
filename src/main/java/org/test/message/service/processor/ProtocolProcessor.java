package org.test.message.service.processor;

import java.util.concurrent.TimeUnit;

import org.test.message.server.netty.channel.ChannelManager;

import io.netty.util.HashedWheelTimer;

public class ProtocolProcessor {

    private ChannelManager channelManager;

    /**
     * 用于重新发送超时的消息
     */
    private HashedWheelTimer reSendWheel;

    public void init(ChannelManager channelManager) {
        this.channelManager = channelManager;
        this.reSendWheel = new HashedWheelTimer(1, TimeUnit.SECONDS, 60);
    }

    public void close() {
        reSendWheel.stop();
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }
}
