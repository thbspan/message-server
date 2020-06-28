package org.test.message.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.test.message.server.config.IConfig;
import org.test.message.server.netty.channel.ChannelManager;
import org.test.message.server.netty.channel.impl.ChannelManagerImpl;
import org.test.message.service.processor.ProtocolProcessor;

public class ProtocolProcessorBootstrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolProcessorBootstrapper.class);

    private final ProtocolProcessor processor;


    public ProtocolProcessorBootstrapper() {
        processor = new ProtocolProcessor();
    }

    public ProtocolProcessor init(IConfig config) {
        ChannelManager channelManager = new ChannelManagerImpl();

        processor.init(channelManager);
        return processor;
    }

    public void shutdown() {
        processor.close();
    }
}
