package NettySocketServer;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

public class SocketServerInitializer extends ChannelInitializer<SocketChannel> {

	private final SslContext sslCtx;
	
	public SocketServerInitializer(SslContext sslCtx){
			this.sslCtx=sslCtx;
	}
	
	@Override
	protected void initChannel(SocketChannel arg0) throws Exception {
		// TODO Auto-generated method stub
		ChannelPipeline pipeline = arg0.pipeline();
		
		//pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		//안드로이드 클라이언트로 하면서 수정한 부분
		pipeline.addLast(new ByteToMessageDecoder() {
            @Override
            public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                out.add(in.readBytes(in.readableBytes()));
            }
        });
		pipeline.addLast(new StringDecoder());
		pipeline.addLast(new StringEncoder());
		pipeline.addLast(new SocketServerHandler());
	}
}
