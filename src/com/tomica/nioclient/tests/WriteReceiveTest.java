package com.tomica.nioclient.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.max.MaxCore;
import org.junit.rules.Stopwatch;

import com.tomica.nioclient.NIOClient;
import com.tomica.nioclient.exceptions.ClientNotConnectedException;
import com.tomica.nioserver.IntentChangeRequest;
import com.tomica.nioserver.NIOServer;
import com.tomica.nioserver.objects.PlayerInfo;

public class WriteReceiveTest implements 
	com.tomica.nioclient.events.OnClientReceivedListener, 
	com.tomica.nioclient.events.OnClientConnectedListener{
	PlayerInfo info;
	NIOServer server;
	
	private NIOClient cl;
	
	private SendStatistics stats = new SendStatistics();
	
	StopWatch w = new StopWatch();
	
	@Before
	public void setUp() throws Exception {
		info = new PlayerInfo(22, "trapstar321", new Date(), (double)1000000000);			
	}
	
	@Test
	public void test() throws IOException, InterruptedException{		
		startClients();	
		
		Thread.sleep(1000);
		
		cl.disconnect();
		
		Thread.sleep(1000);		
			
		System.out.println(stats);
	}
	
	public void startClients() throws IOException{
		com.tomica.nioclient.messages.ClientMessage[] clientMessages = new com.tomica.nioclient.messages.ClientMessage[4];		
		clientMessages[0]=new com.tomica.nioclient.messages.impl.CM_SETPOS();
		clientMessages[1]=new com.tomica.nioclient.messages.impl.CM_ISONLINE();
		clientMessages[2]=new com.tomica.nioclient.messages.impl.CM_PLAYERINFO();
		clientMessages[3]=new com.tomica.nioclient.messages.impl.CM_PING();
		
		com.tomica.nioclient.messages.ServerMessage[] serverMessages = new com.tomica.nioclient.messages.ServerMessage[4];
		serverMessages[0]=new com.tomica.nioclient.messages.impl.SM_ISONLINE();
		serverMessages[1]=new com.tomica.nioclient.messages.impl.SM_LASTPOS();
		serverMessages[2]=new com.tomica.nioclient.messages.impl.SM_PLAYERINFO();
		serverMessages[3]=new com.tomica.nioclient.messages.impl.SM_PONG();
		
		cl = new NIOClient(new InetSocketAddress("localhost", 10023), stats);
		cl.registerClientMessages(clientMessages);
		cl.registerServerMessages(serverMessages);	
		cl.addOnClientConnectedListener(this);
		cl.addOnClientReceivedListener(this);
		
		cl.connect();
	}

	@Override
	public void received(com.tomica.nioclient.events.ClientReceivedEvent event, NIOClient client) {
		w.stop();
		//System.out.println("ClientReceivedEvent: Received message "+event.getMessage()+" from server "+event.getAddress());
		
		stats.reportLastWait(w.getTime());
		
		w.reset();		
		w.start();
		try {
			client.write(new com.tomica.nioclient.messages.ClientMessage[]{new com.tomica.nioclient.messages.impl.CM_PING(new byte[32])});
		} catch (ClientNotConnectedException e) {
			fail("Client not connected");
		}
		
		/*try{
		switch(event.getMessage().getOpCode()){
			case com.tomica.nioclient.messages.impl.SM_ISONLINE.OPCODE:
				
				System.out.println("SM_ISONLINE in "+w.getTime());
				w.reset();
				w.start();
				try {
					client.write(new com.tomica.nioclient.messages.ClientMessage[]{new com.tomica.nioclient.messages.impl.CM_PLAYERINFO(info, true)});
				} catch (ClientNotConnectedException e) {
					fail("Client not connected");
				}
				break;
			case com.tomica.nioclient.messages.impl.SM_PLAYERINFO.OPCODE:
				synchronized (communicationStatus) {
					
				}
				System.out.println("SM_PLAYERINFO in "+w.getTime());
				w.reset();
				w.start();
				try {
					client.write(new com.tomica.nioclient.messages.ClientMessage[]{new com.tomica.nioclient.messages.impl.CM_ISONLINE("jayman")});
				} catch (ClientNotConnectedException e) {
					fail("Client not connected");
				}
				break;
			default:			
				fail("Received unwanted message");
		}
		
		}catch(IOException ex){
			fail("Exception thrown when writting to server");
		}*/
	}

	@Override
	public void connected(com.tomica.nioclient.events.ClientConnectedEvent event, NIOClient client) {			
		w.start();
		//System.out.println("ClientConnectedEvent: Connected to server "+event.getAddress());	
		try {
			client.write(new com.tomica.nioclient.messages.ClientMessage[]{new com.tomica.nioclient.messages.impl.CM_PING(new byte[32])});
		} catch (ClientNotConnectedException e) {
			fail("Client not connected");
		}		
	}
}
