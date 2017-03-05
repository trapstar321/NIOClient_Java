package com.tomica.nioclient.events;

import java.net.InetAddress;

import com.tomica.nioclient.messages.*;

public class ClientReceivedEvent{
	private ServerMessage message;
	private InetAddress address;
	
	public ClientReceivedEvent(InetAddress address, ServerMessage message){
		this.message=message;
		this.address=address;
	}
	
	public ServerMessage getMessage(){
		return message;
	}
	
	public InetAddress getAddress(){
		return address;
	}
}
