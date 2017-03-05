package com.tomica.nioclient.events;

import com.tomica.nioclient.NIOClient;

public interface OnClientReceivedListener{	
	public void received(ClientReceivedEvent event, NIOClient client);
}
