package NettySocketServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class SocketServer {

	private final int port;
	
	//생성자
	public SocketServer(int port){
		super();
		this.port = port;
	}
	
	public static void main(String[] args) throws Exception {
		new SocketServer(5001).run();
	}
	
	public void run() throws Exception{
		// SslContext를 사용하는 경우
		SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder
        		.forServer(ssc.certificate(), ssc.privateKey())
        		.build();
        
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
       
        try{
        	ServerBootstrap bootstrap = new ServerBootstrap();
        	bootstrap.group(bossGroup, workerGroup)
        		 	.channel(NioServerSocketChannel.class)
        		 	.handler(new LoggingHandler(LogLevel.INFO))
        		 	.childHandler(new SocketServerInitializer(sslCtx));
        
        	bootstrap.bind(port).sync().channel().closeFuture().sync();
        }
        finally{
        	bossGroup.shutdownGracefully();
        	workerGroup.shutdownGracefully();
        }
       
	}

}
