package org.test.message.server.netty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.test.message.server.ServerAcceptor;
import org.test.message.server.config.IConfig;
import org.test.message.server.config.ServerConstants;
import org.test.message.server.netty.handler.HttpServerHandler;
import org.test.message.server.netty.handler.NettyMqttHandler;
import org.test.message.server.netty.handler.WebSocketServerHandler;
import org.test.message.server.netty.handler.codec.MessageEncoder;
import org.test.message.service.processor.ProtocolProcessor;
import org.test.message.service.security.SslContextCreator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;

public class NettyAcceptor implements ServerAcceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyAcceptor.class);
    /**
     * 子协议名称
     */
    public static final String SUB_PROTOCOL = "sub_protocol_name";

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Class<? extends ServerSocketChannel> channelClass;
    private int maxMessageByteLength;

    @Override
    public void init(ProtocolProcessor processor, IConfig props, SslContextCreator sslCtxCreator) {
        maxMessageByteLength = props.getProperty(ServerConstants.MAX_MESSAGE_BYTE_LENGTH, ServerConstants.DEFAULT_MAX_MESSAGE_BYTE_LENGTH);
        if (props.getProperty(ServerConstants.NETTY_EPOLL_PROPERTY_NAME, true)) {
            bossGroup = new EpollEventLoopGroup();
            workerGroup = new EpollEventLoopGroup();
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            channelClass = NioServerSocketChannel.class;
        }
        final NettyMqttHandler handler = new NettyMqttHandler(processor);

        initializeWSTransport(handler, props, sslCtxCreator.initSSLContext());
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    interface PipelineInitializer{
        void init(ChannelPipeline pipeline) throws Exception;
    }

    private void initializeWSTransport(NettyMqttHandler mqttHandler, IConfig props, SSLContext sslContext) {
        String host = props.getProperty(ServerConstants.HOST, "localhost");
        int sslPort = props.getProperty(ServerConstants.PORT, 8080);

        MessageEncoder messageEncoder = new MessageEncoder();

        initServer(host, sslPort, pipeline -> {
            if (null != sslContext) {
                pipeline.addLast("ssl", createSslHandler(sslContext, false));
            }
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
            WebSocketServerHandler.Builder builder = WebSocketServerHandler.Builder.newBuilder();
            builder.websocketPath("/ws");
            builder.subprotocol(SUB_PROTOCOL);
            builder.mqttHandler(mqttHandler);
            builder.maxMessageByteLength(maxMessageByteLength);
            builder.messageEncoder(messageEncoder);

            pipeline.addLast("webSocketServerHandler", builder.build());
            pipeline.addLast("httpServerHandler", new HttpServerHandler(mqttHandler));
        });

    }

    private void initServer(String host, int port, PipelineInitializer pipelineInitializer) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(channelClass)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        try {
                            pipelineInitializer.init(pipeline);
                        } catch (Throwable th) {
                            LOGGER.error("pipeline init error. msg={}", th.getMessage(), th);
                            throw th;
                        }
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = b.bind(host, port);
        try {
            f.sync().addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } catch (InterruptedException e) {
            LOGGER.error("init error. msg={}", e.getMessage(), e);
        }
    }
    private ChannelHandler createSslHandler(SSLContext sslContext, boolean needsClientAuth) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        if (needsClientAuth) {
            sslEngine.setNeedClientAuth(true);
        }
        return new SslHandler(sslEngine);
    }
}
