package com.googlecode.protobuf.pro.duplex.example;

import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.google.protobuf.Service;
import com.googlecode.protobuf.pro.duplex.CleanShutdownHandler;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.RpcConnectionEventNotifier;
import com.googlecode.protobuf.pro.duplex.example.PingPong.PingService;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.listener.RpcConnectionEventListener;
import com.googlecode.protobuf.pro.duplex.logging.CategoryPerServiceLogger;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerBootstrap;

public class StoppingPingServer {
	
	private static Log log = LogFactory.getLog(StoppingPingServer.class);
	
	public static void main(String[] args) throws Exception {
		if ( args.length != 2 ) {
			System.err.println("usage: <serverHostname> <serverPort>");
			System.exit(-1);
		}
		String serverHostname = args[0];
		int serverPort = Integer.parseInt(args[1]);
		
    	PeerInfo serverInfo = new PeerInfo(serverHostname, serverPort);
    	
		// RPC payloads are uncompressed when logged - so reduce logging
		CategoryPerServiceLogger logger = new CategoryPerServiceLogger();
		logger.setLogRequestProto(false);
		logger.setLogResponseProto(false);

		// Configure the server.
        DuplexTcpServerBootstrap bootstrap = new DuplexTcpServerBootstrap(
        		serverInfo,
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

		bootstrap.setRpcServerCallExecutor(new ThreadPoolCallExecutor(10, 10));
		bootstrap.setLogger(logger);
		CleanShutdownHandler shutdownHandler = new CleanShutdownHandler();
        shutdownHandler.addResource(bootstrap);
        
        // setup a RPC event listener - it just logs what happens
        RpcConnectionEventNotifier rpcEventNotifier = new RpcConnectionEventNotifier();
        RpcConnectionEventListener listener = new RpcConnectionEventListener() {
			
			@Override
			public void connectionReestablished(RpcClientChannel clientChannel) {
				log.info("connectionReestablished " + clientChannel);
			}
			
			@Override
			public void connectionOpened(RpcClientChannel clientChannel) {
				log.info("connectionOpened " + clientChannel);
			}
			
			@Override
			public void connectionLost(RpcClientChannel clientChannel) {
				log.info("connectionLost " + clientChannel);
			}
			
			@Override
			public void connectionChanged(RpcClientChannel clientChannel) {
				log.info("connectionChanged " + clientChannel);
			}
		};
		rpcEventNotifier.setEventListener(listener);
    	bootstrap.registerConnectionEventListener(rpcEventNotifier);

    	Service pingService = PingService.newReflectiveService(new PingPongServiceFactory.NonBlockingPingServer());
    	bootstrap.getRpcServiceRegistry().registerService(pingService);
    	
    	// Bind and start to accept incoming connections.
        bootstrap.bind();
        log.info("Serving " + bootstrap);
        
        Thread.sleep(10000);
        
        bootstrap.releaseExternalResources();
	}
	
}