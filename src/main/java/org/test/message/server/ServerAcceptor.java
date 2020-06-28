package org.test.message.server;

import org.test.message.server.config.IConfig;
import org.test.message.service.processor.ProtocolProcessor;
import org.test.message.service.security.SslContextCreator;

public interface ServerAcceptor {
    void init(ProtocolProcessor processor, IConfig props, SslContextCreator sslCtxCreator);

    void close();
}
