package org.test.message.server.config;

public class ServerConstants {
    public static final String NETTY_EPOLL_PROPERTY_NAME = "netty.epoll";
    public static final String PORT = "netty.port";
    public static final String HOST = "netty.host";
    public static final String STORAGE_CLASS_NAME = "storage.class";
    public static final String MAX_MESSAGE_BYTE_LENGTH = "message.size";
    /**
     * 默认最大Message byte(256MB)
     */
    public static final int DEFAULT_MAX_MESSAGE_BYTE_LENGTH = 268435455;

    public static final int DEFAULT_HEARTBEAT = 30 * 1000;
}
