package com.tomica.nioclient.messages.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.tomica.nioclient.messages.ServerMessage;
import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.objects.PlayerInfo;

public class SM_PLAYERINFO extends ServerMessage{
	public static final byte OPCODE=(byte)30;	
	private PlayerInfo info;
	private boolean isOnline;
	
	
	public SM_PLAYERINFO(){
		
	}
	
	public SM_PLAYERINFO(byte[] data) throws IOException, ClassNotFoundException{
		this.data = data.clone();
		info = (PlayerInfo)getObject();
		isOnline=getBool();
	}
	
	@Override
	public byte[] getBytes() {
		byte[] data = getData();
		ByteBuffer b = ByteBuffer.allocate(Message.BYTE_BYTES+Message.INT_BYTES+data.length);
		b.putInt(data.length);
		b.put(getOpCode());
		b.put(data);
		return b.array();
	}

	@Override
	public byte getOpCode() {
		return OPCODE;
	}

	public PlayerInfo getPlayerInfo(){
		return info;
	}
	
	public boolean isOnline() {
		return isOnline;
	}
	
	@Override
	public String toString(){
		return "opCode="+String.valueOf(OPCODE)+" playerInfo="+info+", isOnline="+isOnline;
	}
}
