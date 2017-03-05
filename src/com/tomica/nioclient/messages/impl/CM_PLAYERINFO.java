package com.tomica.nioclient.messages.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.tomica.nioclient.messages.ClientMessage;
import com.tomica.nioserver.messages.Message;
import com.tomica.nioserver.objects.PlayerInfo;

public class CM_PLAYERINFO extends ClientMessage{
	public static final byte OPCODE=(byte)29;	
	private PlayerInfo info;
	private boolean isOnline;
	
	public CM_PLAYERINFO() {
	
	}
	
	public CM_PLAYERINFO(PlayerInfo info, boolean isOnline) throws IOException{
		this.info=info;
		this.isOnline=isOnline;
		putObject(info);	
		putBool(isOnline);
	}

	@Override
	public byte getOpCode() {
		return OPCODE;
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
	
	public PlayerInfo getPlayerInfo(){
		return info;
	}
	
	public boolean isOnline(){
		return isOnline;
	}
	
	@Override
	public String toString(){
		return "opCode="+String.valueOf(OPCODE)+" playerInfo="+info+", isOnline="+isOnline;
	}
}
