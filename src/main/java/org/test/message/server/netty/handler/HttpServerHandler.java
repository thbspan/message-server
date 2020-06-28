package org.test.message.server.netty.handler;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.test.message.server.netty.NettyAcceptor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);
    private final NettyMqttHandler mqttHandler;

    public HttpServerHandler(NettyMqttHandler mqttHandler) {
        this.mqttHandler = mqttHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        HttpMethod httpMethod = req.method();

        if (!httpMethod.equals(HttpMethod.GET) && !httpMethod.equals(HttpMethod.POST)) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
        }

        final String requestURI = req.uri();

        try {
            switch (requestURI) {
                case "/":
                    handlerIndexURI(ctx, req);
                    break;
                case "/push":
                    // 处理消息推送请求

                    break;
                default:
                    handler404URI(ctx, req);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("handler uri({}) exception", requestURI, e);
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
        }
    }

    private void handlerIndexURI(ChannelHandlerContext ctx, FullHttpRequest req) {
        ByteBuf content = Unpooled.copiedBuffer("sub protocol name: " + NettyAcceptor.SUB_PROTOCOL, CharsetUtil.US_ASCII);
        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

        res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        HttpUtil.setContentLength(res, content.readableBytes());

        sendHttpResponse(ctx, req, res);
    }

    private void handler404URI(ChannelHandlerContext ctx, FullHttpRequest req){
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        sendHttpResponse(ctx, req, res);
    }
    /**
     * 发送http响应消息
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        if (!HttpResponseStatus.OK.equals(res.status())) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), StandardCharsets.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }
        ctx.channel().writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }
}
