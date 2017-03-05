import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

import com.tomica.nioclient.NIOClient;
import com.tomica.nioclient.events.ClientConnectedEvent;
import com.tomica.nioclient.events.ClientDisconnectedEvent;
import com.tomica.nioclient.events.ClientReceivedEvent;
import com.tomica.nioclient.events.OnClientConnectedListener;
import com.tomica.nioclient.events.OnClientDisconnectedListener;
import com.tomica.nioclient.events.OnClientReceivedListener;
import com.tomica.nioclient.exceptions.ClientNotConnectedException;
import com.tomica.nioclient.messages.*;
import com.tomica.nioclient.messages.impl.*;
import com.tomica.nioserver.objects.PlayerInfo;

public class RunClient implements OnClientConnectedListener, OnClientReceivedListener, OnClientDisconnectedListener{
	private NIOClient client;
	
	
	public static void main(String[] args) throws IOException {
		RunClient m = new RunClient();
		m.client = new NIOClient(new InetSocketAddress("localhost", 10023));
		m.client.addOnClientConnectedListener(m);
		m.client.addOnClientReceivedListener(m);
		m.client.addOnClientDisconnectedListener(m);
		
		ClientMessage[] clientMessages = new ClientMessage[4];		
		clientMessages[0]=new CM_SETPOS();
		clientMessages[1]=new CM_ISONLINE();
		clientMessages[2]=new CM_PLAYERINFO();
		clientMessages[3]=new CM_PING();
		
		ServerMessage[] serverMessages = new ServerMessage[4];
		serverMessages[0]=new SM_ISONLINE();
		serverMessages[1]=new SM_LASTPOS();
		serverMessages[2]=new SM_PLAYERINFO();
		serverMessages[3]=new SM_PONG();
		m.client.registerClientMessages(clientMessages);
		m.client.registerServerMessages(serverMessages);		
		
		m.client.connect();		
	}	
	
	@Override
	public void connected(ClientConnectedEvent event, NIOClient client) {
		System.out.println("ClientConnectedEvent: Connected to server "+event.getAddress());		
		try {			
			//client.write(new ClientMessage[]{new CM_ISONLINE("jayman")});
			//PlayerInfo info = new PlayerInfo(22, "trapstar321", new Date(), (double)1000000000);	
			client.write(new ClientMessage[]{new CM_PING(new byte[32])});
		} catch (ClientNotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*PlayerInfo info = new PlayerInfo(22, "trapstar321", new Date(), (double)1000000000);
		try {
			client.write(new ClientMessage[]{new CM_PLAYERINFO(info, true)});
		} catch (IOException e) {
			NIOClient.log(Level.SEVERE, "IOException: "+e.getMessage(), e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	@Override
	public void received(ClientReceivedEvent event, NIOClient client) {
		System.out.println("ClientReceivedEvent: Received message "+event.getMessage()+" from server "+event.getAddress());
		try{
			Thread.sleep(500);
			client.write(new ClientMessage[]{new CM_PING(new byte[32])});
		}catch(Exception ex){
			ex.printStackTrace();
		}
		/*SM_PLAYERINFO message = (SM_PLAYERINFO) event.getMessage();
		PlayerInfo info = message.getPlayerInfo();
		System.out.println("Player info: "+info);*/
		
		/*try {		
			//Thread.sleep(1000);
			client.write(new ClientMessage[]{new CM_ISONLINE("jayman")});
		} catch (ClientNotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}/* catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	@Override
	public void disconnected(ClientDisconnectedEvent event, NIOClient client) {
		System.out.println("ClientDisconnectedEvent: Disconnected from server "+event.getAddress());			
	}
}
