package org.test.message.server.netty.handler.codec;

public class Message {
    private final MessageHeader header;
    private final byte[] body;

    public Message(MessageHeader header) {
        this(header, null);
    }

    public Message(MessageHeader header, byte[] body) {
        this.header = header;
        this.body = body;
    }

    public MessageHeader getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }
}
