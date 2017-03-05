package com.tomica.nioclient.events;

import java.net.InetAddress;

public class ClientConnectedEvent{
	private InetAddress address;
	
	public ClientConnectedEvent(InetAddress address){
		this.address=address;
	}
	
	public InetAddress getAddress(){
		return address;
	}
}
