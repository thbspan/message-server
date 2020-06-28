package org.test.message.server.netty.handler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.test.message.server.config.ServerConstants;
import org.test.message.server.netty.handler.codec.MessageDecoder;
import org.test.message.server.netty.handler.codec.MessageEncoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class WebSocketServerHandler extends WebSocketServerProtocolHandler {
    private String subprotocol;
    private int maxMessageByteLength;
    private NettyMqttHandler mqttHandler;
    private MessageEncoder messageEncoder;
    private WebSocketServerHandler(String websocketPath, String subprotocol) {
        super(websocketPath, subprotocol);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HandshakeComplete) {
            HandshakeComplete handshakeComplete = (HandshakeComplete) evt;
            if (!StringUtils.equals(subprotocol, handshakeComplete.selectedSubprotocol())) {
                ctx.channel().close();
            } else {
                ChannelPipeline pipeline = ctx.channel().pipeline();
                // 移除http消息处理
                pipeline.remove("httpServerHandler");
                // 添加 WebSocket 消息处理
                pipeline.addLast("ws2bytebufDecoder", new WsToByteBufDecoder());
                pipeline.addLast("bytebuf2wsEncoder", new ByteBufToWsEncoder());
                pipeline.addLast("decoder", new MessageDecoder(maxMessageByteLength));
                pipeline.addLast("encoder", messageEncoder);
                // 三个心跳周期内连接依然是空闲状态，则触发 IdleStateEvent 事件
                pipeline.addLast("server-idle-handler", new IdleStateHandler(0, 0, 3 * ServerConstants.DEFAULT_HEARTBEAT, MILLISECONDS));
                pipeline.addLast("handler", mqttHandler);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    public static class WsToByteBufDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {
        @Override
        protected void decode(ChannelHandlerContext chc, BinaryWebSocketFrame frame, List<Object> out) throws Exception {
            ByteBuf bb = frame.content();
            bb.retain();
            out.add(bb);
        }
    }

    public static class ByteBufToWsEncoder extends MessageToMessageEncoder<ByteBuf> {
        @Override
        protected void encode(ChannelHandlerContext chc, ByteBuf bb, List<Object> out) throws Exception {
            BinaryWebSocketFrame result = new BinaryWebSocketFrame();
            result.content().writeBytes(bb);
            out.add(result);
        }
    }

    public static class Builder {
        private String websocketPath;
        private String subprotocol;
        private int maxMessageByteLength;
        private NettyMqttHandler mqttHandler;
        private MessageEncoder messageEncoder;
        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder websocketPath(String websocketPath) {
            this.websocketPath = websocketPath;
            return this;
        }

        public Builder subprotocol(String subprotocol) {
            this.subprotocol = subprotocol;
            return this;
        }

        public Builder maxMessageByteLength(int maxMessageByteLength) {
            this.maxMessageByteLength = maxMessageByteLength;
            return this;
        }

        public Builder mqttHandler(NettyMqttHandler mqttHandler) {
            this.mqttHandler = mqttHandler;
            return this;
        }

        public Builder messageEncoder(MessageEncoder messageEncoder) {
            this.messageEncoder = messageEncoder;
            return this;
        }
        public WebSocketServerHandler build(){
            WebSocketServerHandler handler = new WebSocketServerHandler(websocketPath, subprotocol);
            handler.subprotocol = subprotocol;
            handler.maxMessageByteLength = maxMessageByteLength;
            handler.mqttHandler = mqttHandler;
            handler.messageEncoder = messageEncoder;
            return handler;
        }
        private Builder() {
        }
    }
}
