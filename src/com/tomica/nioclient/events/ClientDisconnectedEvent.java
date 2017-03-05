package com.tomica.nioclient.events;

import java.net.InetAddress;

public class ClientDisconnectedEvent extends ClientConnectedEvent{

	public ClientDisconnectedEvent(InetAddress address) {
		super(address);		
	}
}
