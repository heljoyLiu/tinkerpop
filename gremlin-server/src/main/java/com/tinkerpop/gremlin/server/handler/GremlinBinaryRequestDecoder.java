package com.tinkerpop.gremlin.server.handler;

import com.tinkerpop.gremlin.driver.MessageSerializer;
import com.tinkerpop.gremlin.driver.message.RequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Decodes the contents of a {@code BinaryWebSocketFrame}.  Binary-based frames assume that the format is encoded
 * in the first initial bytes of the message.  From there the proper serializer can be chosen and the message
 * can then be deserialized.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GremlinBinaryRequestDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    protected void decode(final ChannelHandlerContext channelHandlerContext, final BinaryWebSocketFrame frame, final List<Object> objects) throws Exception {
        final ByteBuf messageBytes = frame.content();
        final byte len = messageBytes.readByte();
        if (len <= 0) {
            objects.add(RequestMessage.INVALID);
            return;
        }

        final ByteBuf contentTypeBytes = channelHandlerContext.alloc().buffer(len);
        try {
            messageBytes.readBytes(contentTypeBytes);
            final String contentType = contentTypeBytes.toString(UTF8);
            final MessageSerializer serializer = MessageSerializer.select(contentType, MessageSerializer.DEFAULT_REQUEST_SERIALIZER);
            objects.add(serializer.deserializeRequest(messageBytes.discardReadBytes()).orElse(RequestMessage.INVALID));
        } finally {
            contentTypeBytes.release();
        }
    }
}
