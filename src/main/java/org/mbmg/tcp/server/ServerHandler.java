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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.CharsetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.mbmg.tcp.util.Parser;
import org.mbmg.tcp.util.Record;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.io.*;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	
    private final GraphiteClient graphiteClient;

    public ServerHandler (String graphiteHost, int graphitePort) {
        graphiteClient = new GraphiteClient(graphiteHost,graphitePort);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            	String receivedContent = in.toString(io.netty.util.CharsetUtil.US_ASCII);
            	new Consumer(receivedContent).start();
            	
            	/*
            	if (!receivedContent.startsWith("@")) {
					// Parse packet
	                Record newRecord = Parser.toRecord(receivedContent);
	                System.out.println(newRecord.toGraphite());
	                
	                // Send data to Carbon
	                
	                List<String> channelData = newRecord.toGraphite();
	                for (String chanelSample : channelData) {
	                    graphiteClient.sendData(chanelSample);
	                }
				}
				*/
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	in.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
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
	                List<String> channelData = newRecord.toGraphite();
	                for (String chanelSample : channelData) {
	                    graphiteClient.sendData(chanelSample);
	                }   
	                
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
        }
    }

    private static class GraphiteClient {

        private static final EventLoopGroup bossGroup = new NioEventLoopGroup();
        private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
        private static final StringEncoder ENCODER = new StringEncoder();
        private static final WriteTimeoutHandler TIMEOUT_HANDLER = new WriteTimeoutHandler(120);
        private static final ServerBootstrap bootstrap = new ServerBootstrap();
        private final String graphiteHost;
        private final int graphitePort;
        private boolean started = false;

        private Channel connection;

        private GraphiteClient(String graphiteHost, int graphitePort) {
            this.graphiteHost = graphiteHost;
            this.graphitePort = graphitePort;
            this.started = false;
        }

        public void startUp() {
            try {
            	bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(ENCODER);
                        ch.pipeline().addLast(TIMEOUT_HANDLER);
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);
                ChannelFuture f = bootstrap.bind(graphitePort).sync();
                this.connection = f.channel();
                started = true;
                f.channel().closeFuture().sync();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
            	workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }

        public void sendData(String data) {
            // Connect lazily to make start work even if graphite isn't up
            if(connection == null || started == false) {
                startUp();
            }
            if (connection != null && connection.isOpen()) {
                connection.writeAndFlush(data);
            }
        }

        public void shutdown() {
            if (connection != null) {
                connection.close().awaitUninterruptibly();
            }
            System.out.println("--- GRAPHITE CLIENT - Stopped.");
        }

    }
}