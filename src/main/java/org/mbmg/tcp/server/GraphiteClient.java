package org.mbmg.tcp.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.WriteTimeoutHandler;

class GraphiteClient {

	private static final EventLoopGroup group = new NioEventLoopGroup();
    private static final StringEncoder ENCODER = new StringEncoder();
    private static final WriteTimeoutHandler TIMEOUT_HANDLER = new WriteTimeoutHandler(120);
    private static final Bootstrap bootstrap = new Bootstrap();
    private boolean started;
    private static String graphiteHost = System.getProperty("org.mbmg.graphite.server.host","localhost");
    private static Integer graphitePort = Integer.parseInt(System.getProperty("org.mbmg.graphite.server.port","2003"));
    private static final GraphiteClient graphiteClient = new GraphiteClient();

    private Channel connection;

    private GraphiteClient() {
        this.started = false;
    }

    public static GraphiteClient getClient() {
    	return graphiteClient;
    }
    
    public void startUp() {
        try {
            if(bootstrap.group() == null) {
                bootstrap.group(group);
            }
            bootstrap.channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
            	@Override
            	public void initChannel(SocketChannel ch) throws Exception {
            		ch.pipeline().addLast(ENCODER);
                    ch.pipeline().addLast(TIMEOUT_HANDLER);
                }
            });
            ChannelFuture f = bootstrap.connect(graphiteHost, graphitePort).sync();
            this.connection = f.channel();
            this.started = true;
            f.channel().closeFuture().sync();        
        } catch (Exception ex) {
        	System.out.println("Exception Start up");
            ex.printStackTrace();
            group.shutdownGracefully();
        } 	        	
    }

    public void sendData(String data) {
        // Connect lazily to make start work even if graphite isn't up
        if(this.started == false) {
            startUp();
        }
        if (this.started == true) {
            this.connection.writeAndFlush(data);
        }
    }

    public void shutdown() {
        if (connection != null) {
            connection.close().awaitUninterruptibly();
        }
        System.out.println("--- GRAPHITE CLIENT - Stopped.");
    }

}