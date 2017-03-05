package com.tomica.nioclient.events;

import com.tomica.nioclient.NIOClient;

public interface OnClientConnectedListener{
	public void connected(ClientConnectedEvent event, NIOClient client);
}