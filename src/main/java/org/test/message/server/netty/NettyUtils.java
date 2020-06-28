package org.test.message.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public final class NettyUtils {
    public static final String KEY_UIN = "uin";
    private static final AttributeKey<String> ATTR_KEY_UIN = AttributeKey.valueOf(KEY_UIN);

    public static String uin(Channel channel) {
        channel.attr(NettyUtils.ATTR_KEY_UIN);
        return channel.attr(NettyUtils.ATTR_KEY_UIN).get();
    }

    public static void uin(Channel channel, String uin) {
        channel.attr(NettyUtils.ATTR_KEY_UIN).set(uin);
    }

    public static byte[] readBytes(ByteBuf buf) {
        if (buf.hasArray()){
            return buf.array();
        } else {
            byte[] payloadContent = new byte[buf.readableBytes()];
            int mark = buf.readerIndex();
            buf.readBytes(payloadContent);
            buf.readerIndex(mark);
            return payloadContent;
        }
    }
    private NettyUtils(){}
}
