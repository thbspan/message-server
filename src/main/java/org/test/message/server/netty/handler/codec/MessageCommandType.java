package org.test.message.server.netty.handler.codec;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息指令
 */
public enum MessageCommandType {
    CONNECT(1),
    CONNECT_ACK(2),
    PUBLISH(3),
    PUBLISH_ACK(4),
    PING(5),
    PONG(6),
    DISCONNECT(7),
    UNKNOWN(-1);

    private final int value;

    MessageCommandType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    private static final Map<Integer, MessageCommandType> types = new HashMap<>();

    static {
        for (MessageCommandType type : values()) {
            types.put(type.value, type);
        }
    }

    public static MessageCommandType valueOf(int value) {
        return types.getOrDefault(value, UNKNOWN);
    }
}
