package org.mbmg.tcp.server;
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.bootstrap.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.StringUtil;

import org.mbmg.tcp.util.Parser;
import org.mbmg.tcp.util.Record;

import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
    private final GraphiteClient graphiteClient;

    public ServerHandler (String graphiteHost, int graphitePort, boolean started) {
        graphiteClient = new GraphiteClient(graphiteHost, graphitePort, started);
        System.out.println("Constructor ServerHandler, create new Graphite object");
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
        	System.out.println("channelRead");
        	String receivedContent = in.toString(io.netty.util.CharsetUtil.US_ASCII);
            new Consumer(receivedContent).start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	in.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
    	System.out.println("Exception caught in ServerHandler");
    	graphiteClient.shutdown();
        cause.printStackTrace();
        ctx.close();
    }

    private class Consumer extends Thread {
    	
    	private final String receivedContent;

    	public Consumer(String receivedContent) {
    		this.receivedContent = receivedContent;
    	}
    	
		@Override
        public void run() {
        	try{
        		if (!receivedContent.startsWith("@")) {
					// Parse packet
	                Record newRecord = Parser.toRecord(receivedContent);
	                System.out.println(newRecord.toGraphite());
	                
	                // Send data to Carbon
	                /*
	                List<String> channelData = newRecord.toGraphite();
	                for (String chanelSample : channelData) {
	                    graphiteClient.sendData(chanelSample);
	                }   
	                */
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
        }
    }

    private static class GraphiteClient {

    	private static final EventLoopGroup group = new NioEventLoopGroup();
    	private volatile ChannelFactory<? extends Channel> channelFactory;
        private static final StringEncoder ENCODER = new StringEncoder();
        private static final WriteTimeoutHandler TIMEOUT_HANDLER = new WriteTimeoutHandler(120);
        private static final Bootstrap bootstrap = new Bootstrap();
        private final String graphiteHost;
        private final int graphitePort;
        private boolean started;

        private Channel connection;

        private GraphiteClient(String graphiteHost, int graphitePort, boolean started) {
            this.graphiteHost = graphiteHost;
            this.graphitePort = graphitePort;
            this.started = started;
        }
      
        public void startUp() {
        	System.out.println("Enter StartUp");

            try {
                if(bootstrap.group() == null) {
                    bootstrap.group(group);
                }
                channelFactory = new BootstrapChannelFactory<Channel>(NioSocketChannel.class);
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
            	System.out.println("Start Connection");
                startUp();
            }
            if (this.started == true) {
            	System.out.println("Connection already started");
                this.connection.writeAndFlush(data);
            }
        }

        public void shutdown() {
            if (connection != null) {
            	channelFactory = null;
                connection.close().awaitUninterruptibly();
                System.out.println("--- GRAPHITE CLIENT - Closing connection.");
            }
            System.out.println("--- GRAPHITE CLIENT - Stopped.");
        }

    }
    /*
    private static final class BootstrapChannelFactory<T extends Channel> implements ChannelFactory<T> {
    	private final Class<? extends T> clazz;
    		BootstrapChannelFactory(Class<? extends T> clazz) {
    			this.clazz = clazz;
    		}
    		@Override
    		public T newChannel() {
    			try {
    				return clazz.newInstance();
    			} catch (Throwable t) {
    				throw new ChannelException("Unable to create Channel from class " + clazz, t);
    			}
    		}
    		@Override
    		public String toString() {
    			return StringUtil.simpleClassName(clazz) + ".class";
    		}
    }
    */
}