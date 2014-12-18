package org.mbmg.tcp.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.*;

public class Server { 
	
	private final int port;
    private final String graphiteHost;
    private final int graphitePort;

    public Server(int port, String graphiteHost, int graphitePort) {
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
        this.port = port;
    }
	
	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
        	ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new ServerHandler(graphiteHost, graphitePort));
                     // Decoders
                     //ch.pipeline().addLast("frameDecoder", new LineBasedFrameDecoder(80));
                     //ch.pipeline().addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
	}
		
	public static void main(String[] args) throws Exception {
		int port;
        int graphitePort;
        String graphiteHost;
        // Retrieve port specified in System properties. Used port 6001 as default
        port = Integer.parseInt(System.getProperty("org.mbmg.tcp.server.port","6001"));
        graphiteHost = System.getProperty("org.mbmg.graphite.server.host","localhost");
        graphitePort = Integer.parseInt(System.getProperty("org.mbmg.graphite.server.port","2003"));
        new Server(port, graphiteHost, graphitePort).run();
	}
}