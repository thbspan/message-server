package org.test.message.server.netty.handler;

import org.test.message.service.processor.ProtocolProcessor;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

@ChannelHandler.Sharable
public class NettyMqttHandler extends ChannelInboundHandlerAdapter {

    public NettyMqttHandler(ProtocolProcessor processor) {

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // server will close channel when server don't receive any heartbeat from client util timeout.
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
        }
        super.userEventTriggered(ctx, evt);
    }
}
