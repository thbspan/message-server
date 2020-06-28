package org.test.message.server.netty.handler.codec;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

@ChannelHandler.Sharable
public class MessageEncoder extends MessageToMessageEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        MessageHeader header = msg.getHeader();
        int variableLength = getVariableLengthInt(header.getRemainingLength());
        byte[] body = msg.getBody();
        int bodyLength = ArrayUtils.getLength(body);
        // 1字节指令 + 4位重传次数 + 4位保留位 + 可变长度 + variableLength + body长度
        ByteBuf byteBuf = ctx.alloc().buffer(2 + variableLength + bodyLength);

        byteBuf.writeByte(header.getType().getValue());
        byteBuf.writeByte(header.getRetryCount() << 4 & header.getReserved());
        writeVariableLengthInt(byteBuf, header.getRemainingLength());
        if (bodyLength > 0) {
            byteBuf.writeBytes(msg.getBody());
        }
        out.add(byteBuf);
    }

    private int getVariableLengthInt(int remainingLength) {
        return 0;
    }

    /**
     * 参考proto中可变长度int，能够根据num值的大小，动态的控制写入的字节数，避免一直占用4个字节，增加消息的长度
     */
    private static void writeVariableLengthInt(ByteBuf byteBuf, int num) {
        do {
            int digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            byteBuf.writeByte(digit);
        } while (num > 0);
    }
}
