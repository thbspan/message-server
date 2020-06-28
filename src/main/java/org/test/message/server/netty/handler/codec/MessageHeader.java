package org.test.message.server.netty.handler.codec;

/**
 * 消息头部
 * @author panshen
 */
public class MessageHeader {
    private final MessageCommandType type;
    /**
     * 重试次数 4bit
     */
    private final byte retryCount;
    /**
     * 保留位 4bit
     */
    private final byte reserved;

    private final int remainingLength;

    public MessageHeader(MessageCommandType type, byte retryCount, byte reserved, int remainingLength) {
        this.type = type;
        this.retryCount = retryCount;
        this.reserved = reserved;
        this.remainingLength = remainingLength;
    }

    public MessageHeader(MessageCommandType type, int retryCount, int reserved, int remainingLength) {
        this.type = type;
        this.retryCount = (byte) retryCount;
        this.reserved = (byte) reserved;
        this.remainingLength = remainingLength;
    }

    public MessageCommandType getType() {
        return type;
    }

    public byte getRetryCount() {
        return retryCount;
    }

    public byte getReserved() {
        return reserved;
    }

    public int getRemainingLength() {
        return remainingLength;
    }

    @Override
    public String toString() {
        return "MessageHeader{" +
                "type=" + type +
                ", retryCount=" + retryCount +
                ", reserved=" + reserved +
                ", remainingLength=" + remainingLength +
                '}';
    }
}
