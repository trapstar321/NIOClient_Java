package com.tomica.nioclient.events;

import com.tomica.nioclient.NIOClient;

public interface OnClientDisconnectedListener{
	public void disconnected(ClientDisconnectedEvent event, NIOClient client);
}