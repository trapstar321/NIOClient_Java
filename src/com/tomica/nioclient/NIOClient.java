package com.tomica.nioclient;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.StopWatch;

import com.tomica.nioclient.events.ClientConnectedEvent;
import com.tomica.nioclient.events.ClientDisconnectedEvent;
import com.tomica.nioclient.events.ClientReceivedEvent;
import com.tomica.nioclient.events.OnClientConnectedListener;
import com.tomica.nioclient.events.OnClientDisconnectedListener;
import com.tomica.nioclient.events.OnClientReceivedListener;
import com.tomica.nioclient.exceptions.ClientNotConnectedException;
import com.tomica.nioclient.messages.*;
import com.tomica.nioclient.tests.SendStatistics;
import com.tomica.nioclient.IntentChangeRequest;

public class NIOClient implements Runnable{
    private static Logger logger;
    
	{
		logger = Logger.getLogger(NIOClient.class.getName());
        logger.setUseParentHandlers(false);

        LogFormatter formatter = new LogFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);

        logger.addHandler(handler);
	}
	
	private Selector selector = Selector.open();
    private SocketChannel connection = SocketChannel.open();  
    public static boolean LOG = false;
    
	private ClientMessage[] clientMessages=null;
	private ServerMessage[] serverMessages=null;
    
    private List<ByteBuffer> writeQueue = new LinkedList<ByteBuffer>();
	private List<IntentChangeRequest> intentRequests = new ArrayList<IntentChangeRequest>();
	
	private Thread worker;
	private SocketAddress remote;
	
	private Object connectionStateLock = new Object();
	private boolean connected;
	private boolean disconnect=false;
	private boolean connect=false;
	
	private Object selectorLock = new Object();
	
	private StopWatch watch = new StopWatch();
	private StopWatch runWatch = new StopWatch();
	private SendStatistics stats = new SendStatistics();
	
	public NIOClient(SocketAddress remote) throws IOException{    	 
    	this.remote=remote;    	
    }
	
	public NIOClient(SocketAddress remote, SendStatistics stats) throws IOException{    	 
    	this.remote=remote;  
    	this.stats=stats;
    }
	
	private List<OnClientReceivedListener> clientReceivedListeners = new ArrayList<OnClientReceivedListener>();
	private List<OnClientConnectedListener> clientConnectedListeners = new ArrayList<OnClientConnectedListener>();
	private List<OnClientDisconnectedListener> clientDisconnectedListeners = new ArrayList<OnClientDisconnectedListener>();
	
	public void addOnClientReceivedListener(OnClientReceivedListener listener){
		synchronized (clientReceivedListeners) {
			clientReceivedListeners.add(listener);	
		}		
	}
	
	public void removeOnClientReceivedListner(OnClientReceivedListener listener){
		synchronized (clientReceivedListeners) {
			clientReceivedListeners.remove(listener);	
		}		
	}
	
	public void addOnClientConnectedListener(OnClientConnectedListener listener){
		synchronized (clientConnectedListeners) {
			clientConnectedListeners.add(listener);	
		}		
	}
	
	public void removeOnClientConnectedListner(OnClientConnectedListener listener){
		synchronized (clientConnectedListeners) {
			clientConnectedListeners.remove(listener);	
		}		
	}
	
	public void addOnClientDisconnectedListener(OnClientDisconnectedListener listener){
		synchronized (clientDisconnectedListeners) {
			clientDisconnectedListeners.add(listener);	
		}		
	}
	
	public void removeOnClientDisconnectedListner(OnClientDisconnectedListener listener){
		synchronized (clientDisconnectedListeners) {
			clientDisconnectedListeners.remove(listener);	
		}		
	}
    
	public void run() {
		while(true){
			runWatch.reset();
			runWatch.start();			
			try{
				
				synchronized (connectionStateLock) {
					if(disconnect){
						log(Level.INFO, "Exit worker thread");
						close();
						return;
					}
				}
				
				//change channel intent keys
	        	synchronized(intentRequests) {
	        		Iterator<IntentChangeRequest> changes = this.intentRequests.iterator();
	        		while (changes.hasNext()) {
	        			IntentChangeRequest change = (IntentChangeRequest) changes.next();                      
	        			SelectionKey key = connection.keyFor(this.selector);
	        			key.interestOps(change.getIntent());
	        			
	        			switch(change.getIntent()){
		        	  	case SelectionKey.OP_READ:
		        	  		log(Level.INFO, "Changed intentOps to OP_READ for connection "+connection);
		        	  		break;
		        	  	case SelectionKey.OP_WRITE:
		        	  		log(Level.INFO, "Changed intentOps to OP_WRITE for connection "+connection);
		        	  		break;
		        	  	default:
		        	  		log(Level.INFO, "Changed intentOps to "+change.getIntent()+" for connection "+connection);
		        	  		break;
		        	  }		       
	        		}
	        		this.intentRequests.clear();
	        	}
	        	watch.reset();
				watch.start();
				selector.select();
				watch.stop();
				updateSelectStatistics(watch.getTime());
		        Iterator<SelectionKey> i = selector.selectedKeys().iterator();
		        while (i.hasNext()) {
		            SelectionKey key = i.next();
		            i.remove();
		            if (!key.isValid()) {
		                continue;
		            }
		            try {                    	
		                // get a new connection
		                if (key.isConnectable()) {
		                    // finish connect
		                	SocketChannel channel = (SocketChannel) key.channel();
		                    while (channel.isConnectionPending()) {
		                      channel.finishConnect();
		                      synchronized (connectionStateLock) {
								connected=true;
								disconnect=false;
								connect=false;
		                      }
		                    }
		                    connection.configureBlocking(false);
		                    connection.register(selector, SelectionKey.OP_READ);
		                    
		                    notifyClientConnectedListeners();
		                    
		                    log(Level.INFO, "Connected to "+connection.socket());
		                    // read from the connection
		                } else if (key.isReadable()) {	                	
		                	read();
		                }else if(key.isWritable()){
		                	write();
		                }
		            } catch (Exception ex) {
		            	log(Level.SEVERE, "Error handling connection: " + key.channel(), ex);                                           
		            }
		        }
		        synchronized (selectorLock) {
					
				}
		    } catch (IOException ex) {
		        // call it quits
		        //shutdown();
		        // throw it as a runtime exception so that Bukkit can handle it
		    	log(Level.SEVERE, "IOException: "+ex.getMessage(), ex);
		        throw new RuntimeException(ex);
		    }
			runWatch.stop();
			updateRunStatistics(runWatch.getTime());
		}
	}
	
	public void registerServerMessages(ServerMessage[] messages){
		this.serverMessages=messages.clone();
	}
	
	public void registerClientMessages(ClientMessage[] messages){
		this.clientMessages=messages.clone();
	}
	
	public synchronized void notifyClientConnectedListeners(){
		synchronized (clientConnectedListeners) {
			ClientConnectedEvent event = new ClientConnectedEvent(connection.socket().getInetAddress());
			for(OnClientConnectedListener listener: clientConnectedListeners){
				listener.connected(event, this);
			}	
		}
	}
	
	public synchronized void notifyClientDisconnectedListeners(){
		synchronized (clientDisconnectedListeners) {
			ClientDisconnectedEvent event = new ClientDisconnectedEvent(connection.socket().getInetAddress());
			for(OnClientDisconnectedListener listener: clientDisconnectedListeners){
				listener.disconnected(event, this);
			}	
		}
	}
	
	public synchronized void notifyClientReceivedListeners(byte opCode, byte[] data){
		ServerMessage msg=null;
		if(serverMessages!=null){
			for(ServerMessage b: serverMessages){
				if(b.getOpCode()==opCode){
					msg = b;
					break;
				}
			}
			if(msg==null){
				log(Level.WARNING, "opCode "+opCode+" not registered. No event will be generated");
				return;
			}
		}else{
			log(Level.WARNING, "No server messages has been registered. All received messages will be ignored");
		}
		
		if(msg!=null){
			ServerMessage message = makeMessage(msg, opCode, data);
			ClientReceivedEvent event = new ClientReceivedEvent(connection.socket().getInetAddress(), message);			
			
			log(Level.INFO, "New message from "+connection.socket()+": "+message);			
			
			synchronized (clientReceivedListeners) {
				for(OnClientReceivedListener listener: clientReceivedListeners){			
					
					listener.received(event, this);
				}	
			}			
		}
	}
	
	private ServerMessage makeMessage(ServerMessage msg, byte opCode, byte[] data){
		try {
			Class<?> clazz = msg.getClass();
			Constructor<?> ctor = clazz.getConstructor(byte[].class);
			Object object = ctor.newInstance(new Object[] { data });
			return (ServerMessage)object;		
		} catch (IllegalArgumentException e) {
			log(Level.WARNING,"IllegalArgumentException: "+e.getMessage() ,e);
		} catch (InstantiationException e) {
			log(Level.WARNING,"InstantiationException: "+e.getMessage() ,e);			
		} catch (IllegalAccessException e) {
			log(Level.WARNING,"IllegalAccessException: "+e.getMessage() ,e);
		} catch (InvocationTargetException e) {
			log(Level.WARNING,"InvocationTargetException: "+e.getMessage() ,e);
		} catch (SecurityException e) {
			log(Level.WARNING,"SecurityException: "+e.getMessage() ,e);
		} catch (NoSuchMethodException e) {
			log(Level.WARNING,"NoSuchMethodException: "+e.getMessage() ,e);
		}
		return null;
	}
	
	private void write(){
		watch.reset();
		watch.start();
		ByteBuffer buffer = getWriteBuffer();		

		try{
			while (buffer.position() > 0)
			{
				try
				{
					buffer.flip();
					int count = connection.write(buffer);
					if (count == 0)
					{
						IntentChangeRequest request = new IntentChangeRequest(SelectionKey.OP_WRITE);
						addIntentChangeRequest(request);
						break;
					}
				}
				finally
				{
					buffer.compact();
				}
			}
			if (buffer.position() == 0)
			{
				IntentChangeRequest request = new IntentChangeRequest(SelectionKey.OP_READ);
				addIntentChangeRequest(request);	
			}
		}catch(IOException ex){
			log(Level.SEVERE, "IOException:  "+ex.getMessage()+" while sending message to client "+connection, ex);
			disconnect();				
		}
		watch.stop();
		updateWriteStatistics(watch.getTime());
	}
	
	/*private void write(){		
		watch.reset();
		watch.start();
		while(!isWriteQueueEmpty()){
			ByteBuffer message = getMessage(0);
			
			try{
				int wrote = connection.write(message);
			
				//done so remove
				if(message.remaining()==0){			
					log(Level.INFO, "Sent message to server "+connection);
					removeMessage(0);
					continue;		
				//wait for selector for next op_write
				}else if(wrote==0 && message.remaining()>0){
					log(Level.INFO, "Wrote 0 bytes to server "+connection);
					break;
				//not all written continue
				}else if(wrote>0 && message.remaining()>0){
					log(Level.INFO, "Wrote "+wrote+" from "+message.remaining() +" to server "+connection);
					continue;
				}	
			}catch(IOException ex){
				log(Level.SEVERE, "IOException:  "+ex.getMessage()+" while sending message to server "+connection, ex);
				disconnect();
				return;
			}
		}
		
		if(isWriteQueueEmpty()){
			IntentChangeRequest request = new IntentChangeRequest(SelectionKey.OP_READ);
			addIntentChangeRequest(request);
		}
		watch.stop();
		updateWriteStatistics(watch.getTime());
	}*/
	
	private int bufferSize=2048;
	private ByteBuffer buffer=ByteBuffer.allocate(bufferSize);	
	
	public ByteBuffer getWriteBuffer(){
		synchronized (writeQueue) {
			int size=0;
			for(int i=0; i<writeQueue.size(); i++)
				size+=writeQueue.get(i).capacity();
			
			ByteBuffer b = ByteBuffer.allocate(size);
			
			while(!writeQueue.isEmpty()){
				ByteBuffer data = writeQueue.get(0);
				b.put(data.array());
				writeQueue.remove(0);
			}
			return b;
		}
	}
	
	private void read(){
		watch.reset();
		watch.start();
		SocketChannel client = connection;
		
		if(!client.isOpen()){
			log(Level.INFO, "Channel closed, exit task");
			return;
		}
		
		try {
			int read = client.read(buffer);
			//NIOServer.log(Level.INFO, "Read "+read+" bytes from client "+client.socket());
			
			if(read==-1)
				close();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}

		int dataSize=buffer.position();
		byte[] data = buffer.array();
        		
		if(dataSize==buffer.limit()){
			//enlarge buffer and restore data
			byte[] b = buffer.array();
			buffer = ByteBuffer.allocate(buffer.limit()+bufferSize);
			buffer.put(b);
        }	
		
		buffer.flip();

		int length;
		byte opcode;
		byte[] messageData;
		
		boolean resetBuffer = true;
		
		while(buffer.position()<buffer.limit()){			
			int left = buffer.limit()-buffer.position();
			
			//ima header
			if(left>=5){
				length = buffer.getInt();
				opcode = buffer.get();
				
				//ima i podataka
				if(left-5>=length){
					messageData = new byte[length];
					buffer.get(messageData, 0, length);
				
					log(Level.INFO, "Read message, opcode="+opcode+", length="+length+" data="+messageDataToString(messageData));					
					log(Level.INFO, "At position: "+buffer.position());
					notifyClientReceivedListeners(opcode, messageData);
				//nema podataka pa utrpaj ostatak u buffer	
				}else{
					log(Level.INFO, "Opcode="+opcode+", length="+length+". Whole message not yet received");					
					log(Level.INFO, "At position: "+buffer.position());
					
					log(Level.INFO, "Message not complete, put received back to buffer");
					
					buffer.position(buffer.position()-5);
					
					byte[] newMsgData = new byte[buffer.limit()-buffer.position()];            
	                
		        	System.arraycopy(buffer.array(),
		        			buffer.position(),
		        			newMsgData,
		        			0,
		        			buffer.limit()-buffer.position());	                 	             
		        
		        	//moguæe da je buffer full proširen pa ima više byte-ova u poruci nego šta stane u buffer
		        	if(left>buffer.limit())
		        		buffer=ByteBuffer.allocate(left);
		        	//isto moguæe da je buffer proširen jer ima još byte-ova a poruka nije gotova
		        	else if(buffer.capacity()>bufferSize)
		        		buffer=ByteBuffer.allocate(buffer.capacity());
		        	else
		        		buffer=ByteBuffer.allocate(bufferSize);
		            
		        	buffer.put(newMsgData);
		        	resetBuffer = false;
		            break;
				}
			//nema headera, utrpaj ostatak u buffer
			}else{				
				log(Level.INFO, "Message not complete, put received back to buffer");
				
				byte[] newMsgData = new byte[buffer.limit()-buffer.position()];            
                
	        	System.arraycopy(buffer.array(),
	        			buffer.position(),
	        			newMsgData,
	        			0,
	        			buffer.limit()-buffer.position());	                 	             
	        	
	        	//moguæe da je buffer full proširen pa ima više byte-ova u poruci nego šta stane u buffer
	        	if(left>buffer.limit())
	        		buffer=ByteBuffer.allocate(left);
	        	//isto moguæe da je buffer proširen jer ima još byte-ova a poruka nije gotova
	        	else if(buffer.capacity()>bufferSize)
	        		buffer=ByteBuffer.allocate(buffer.capacity());
	        	else
	        		buffer=ByteBuffer.allocate(bufferSize);
				
	        	buffer.put(newMsgData);
	        	resetBuffer = false;
				break;
			}			
		}
		
		if(resetBuffer){
			log(Level.INFO, "Reset buffer");
			buffer = ByteBuffer.allocate(bufferSize);
		}		
		data = buffer.array();		
		log(Level.INFO, "Buffer: "+messageDataToString(data));
		
		watch.stop();		
		updateReadStatistics(watch.getTime());
	}
	
	private String messageDataToString(byte[] data){
		StringBuilder b = new StringBuilder();
		b.append("[");
		
		for(int i=0;i<data.length; i++){
			if(i<data.length-1){
				b.append(data[i]+",");
			}else{
				b.append(data[i]);
			}
		}	
		
		b.append("]");
		return b.toString();
	}
	
	public void connect() throws IOException{
		synchronized (connectionStateLock) {
			if(!connect && connected){
				log(Level.INFO, "Client already connected");
				return;
			}
			if(!connect){
				log(Level.INFO, "Connect client");
				connect=true;
			}else{
				log(Level.INFO, "Already connecting");
				return;
			}
		}
		connection=SocketChannel.open();
		connection.configureBlocking(false);    	
    	connection.connect(remote);   	
    	 	
    	connection.register(selector, SelectionKey.OP_CONNECT);    	
    	
    	worker = new Thread(this);
    	worker.start();
	}
	
	private void close(){		
		if(connection.isOpen()){			
			log(Level.INFO, "Disconnected from "+connection.socket());
			try{				
				connection.close();
				synchronized (connectionStateLock) {
					connected=false;
					disconnect=false;
				}
				notifyClientDisconnectedListeners();
			}catch(IOException ex){
				log(Level.SEVERE, "IOException: "+ex.getMessage(), ex);
			}			
		}
	}
	
	public void disconnect(){
		synchronized (connectionStateLock) {
			if(connected){
				disconnect=true;
			}else{
				log(Level.INFO, "Client not connected");
			}
		}
	}
	
	public boolean isConnected(){
		synchronized (connectionStateLock) {
			return connected;
		}
	}
	
	public void write(ClientMessage[] messages) throws ClientNotConnectedException{
		
		if(!isConnected())
			throw new ClientNotConnectedException("Client not connected to server");
    	
		if(clientMessages!=null){
			synchronized (writeQueue) {
				ByteBuffer[] data = new ByteBuffer[messages.length];
				
				for(int i=0; i<messages.length;i++)
					data[i]=ByteBuffer.wrap(messages[i].getBytes());
				
				synchronized (writeQueue) {
					writeQueue.addAll(Arrays.asList(data));	
				}		
				
				log(Level.INFO, "Added "+messages.length+" messages to write queue");
	    		
	    		IntentChangeRequest request = new IntentChangeRequest(SelectionKey.OP_WRITE);    		
	    		addIntentChangeRequest(request);
			}
		}else{
			log(Level.WARNING, "No client message has been registered. Will not write to server");
		}
    }

	public ByteBuffer getMessage(int i){		
		synchronized (writeQueue) {
			return writeQueue.get(i);
		}
	}
	
	public ByteBuffer removeMessage(int i){		
		synchronized (writeQueue) {
			return writeQueue.remove(i);
		}
	}
	
	public boolean isWriteQueueEmpty(){
		synchronized (writeQueue) {
			return writeQueue.isEmpty();
		}
	}
	
   public void addIntentChangeRequest(IntentChangeRequest request){
    	synchronized(intentRequests) {
	    	intentRequests.add(request);	    	
    	}
    	
    	synchronized (selectorLock) {
    		selector.wakeup();
		}
    }
	
	private void log(Level level, String message) {		
		if(LOG)
			logger.log(level, message);
	}
	
	private void log(Level level, String message, Throwable ex) {
		if(LOG)
			logger.log(level, message, ex);
	}
	
	private void updateWriteStatistics(long time){
		if(stats!=null){
			stats.reportLastWrite(time);
		}
	}
	
	private void updateReadStatistics(long time){
		if(stats!=null){
			stats.reportLastRead(time);
		}
	}
	
	private void updateSelectStatistics(long time){
		if(stats!=null){
			stats.reportLastSelect(time);
		}
	}
	
	private void updateRunStatistics(long time){
		if(stats!=null){
			stats.reportLastRun(time);
		}
	}
}
