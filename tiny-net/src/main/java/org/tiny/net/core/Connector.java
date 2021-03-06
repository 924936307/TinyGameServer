package org.tiny.net.core;

import org.tiny.net.log.TinyLogger;
import org.tiny.net.thread.NamedThreadFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * client
 * 
 * @author chunbo
 */
public class Connector extends AbstractIoService{
	
	private String ip;
	private int port = 8800;

	// 配置客户端NIO线程组
	private EventLoopGroup group = new NioEventLoopGroup(0, new NamedThreadFactory("co-parent"));

	public Connector(String name, String ip, int port, AbstractChannelInitializer channelInitializer, AbstractChannelHandlerAdapter channelHandler) {
		super(name, channelInitializer, channelHandler);
		
		this.ip = ip;
		this.port = port;
	}
	
	@Override
	public void start() {

		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.SO_KEEPALIVE, true)
				// IO 超时，  还有一种是 Channel超时，
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
							ch.pipeline().addLast(new ProtobufEncoder());
							ch.pipeline().addLast(channelInitializer);
						}
					});

			// 发起异步连接操作
			ChannelFuture f = b.connect(ip, port).sync();
			
			/**  Channel超时， 不代表IO超时， 如果只是Channel超时，并且没有关闭资源链接，可能后续IO仍会连上，会导致严重的问题
			ChannelFuture f = b.connect(ip, port);
			f.awaitUninterruptibly(1000, TimeUnit.MILLISECONDS);
			if (f.isCancelled()) {
				
			} else if (!f.isSuccess()) {
				
			} else {
				// success
			}
			*/
			
			if (f.isSuccess()) {
				TinyLogger.LOG.info("connect {}:{} success ", ip, port);
			}

			// 当客户端链路关闭
			// f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
			TinyLogger.LOG.error("connect{}:{} fail ", ip, port);
			close();
		}
	}
	
	@Override
	public void close() {
		// 优雅退出，释放NIO线程组
		group.shutdownGracefully();
	}
}
