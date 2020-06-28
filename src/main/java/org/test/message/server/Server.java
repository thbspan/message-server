package org.test.message.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.test.message.server.config.IConfig;
import org.test.message.server.config.PropertiesResourceLoader;
import org.test.message.server.netty.NettyAcceptor;
import org.test.message.service.impl.ProtocolProcessorBootstrapper;
import org.test.message.service.processor.ProtocolProcessor;
import org.test.message.service.security.DefaultSslContextCreator;
import org.test.message.service.security.SslContextCreator;

public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private volatile boolean initialized;

    private ProtocolProcessorBootstrapper processorBootstrapper;
    private ServerAcceptor acceptor;

    public void startServer() {
        IConfig config = new PropertiesResourceLoader();
        startServer(config);
    }

    public void startServer(IConfig config) {
        LOGGER.info("server starting...");
        SslContextCreator sslCtxCreator = new DefaultSslContextCreator();
        processorBootstrapper = new ProtocolProcessorBootstrapper();

        final ProtocolProcessor processor = processorBootstrapper.init(config);

        acceptor = new NettyAcceptor();
        acceptor.init(processor, config, sslCtxCreator);
        initialized = true;
        LOGGER.info("server started");
    }

    public void stopServer() {
        if (initialized) {
            LOGGER.info("server stopping...");
            acceptor.close();
            processorBootstrapper.shutdown();
            initialized = false;
            LOGGER.info("server stopped");
        }
    }

    public static void main(String[] args) {
        final Server server = new Server();
        server.startServer();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer, "stop-server"));
    }
}
