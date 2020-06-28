package org.test.message.server.netty.handler.codec;

import java.util.List;

import org.test.message.server.netty.NettyUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;

public class MessageDecoder extends ReplayingDecoder<MessageDecoder.DecoderState> {
    enum DecoderState {
        HEAD,
        BODY,
        BAD_MESSAGE,
    }

    private final int maxMessageByteLength;

    private MessageHeader header;
    private int remainingLength;

    public MessageDecoder(int maxMessageByteLength) {
        super(DecoderState.HEAD);
        this.maxMessageByteLength = maxMessageByteLength;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEAD:
                try {
                    header = decodeHeader(in);
                    remainingLength = header.getRemainingLength();
                    checkpoint(DecoderState.BODY);
                } catch (Exception e) {
                    checkpoint(DecoderState.BAD_MESSAGE);
                    return;
                }
            case BODY:
                try {
                    if (remainingLength > maxMessageByteLength) {
                        throw new DecoderException("too large message: " + remainingLength + " bytes");
                    }
                    byte[] messageBody = NettyUtils.readBytes(in.readRetainedSlice(remainingLength));
                    checkpoint(DecoderState.HEAD);
                    out.add(new Message(header, messageBody));
                    header = null;
                    remainingLength = 0;
                } catch (Exception e) {
                    checkpoint(DecoderState.BAD_MESSAGE);
                    return;
                }
            case BAD_MESSAGE:
                in.skipBytes(actualReadableBytes());
                break;
            default:
                break;
        }
    }

    private MessageHeader decodeHeader(ByteBuf byteBuf) {
        MessageCommandType type = MessageCommandType.valueOf(byteBuf.readUnsignedByte());
        if (type == MessageCommandType.UNKNOWN) {
            throw new DecoderException("unexpected command type");
        }

        short b2 = byteBuf.readUnsignedByte();
        byte retryCount = (byte) (b2 >>> 4);
        byte reversed = (byte) (b2 & 0x0F);

        int remainingLength = 0;
        int multiplier = 1;
        short digit;
        int loops = 0;
        do {
            digit = byteBuf.readUnsignedByte();
            remainingLength += (digit & 127) * multiplier;
            multiplier *= 128;
            loops++;
        } while ((digit & 128) != 0 && loops < 4);

        if (loops == 4 && (digit & 128) != 0) {
            throw new DecoderException("remaining length exceeds 4 digits ");
        }
        return new MessageHeader(type, retryCount, reversed, remainingLength);
    }
}
