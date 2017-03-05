package com.tomica.nioclient.messages.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.tomica.nioclient.messages.ServerMessage;
import com.tomica.nioserver.messages.Message;

public class SM_PONG extends ServerMessage{
	public static final byte OPCODE=(byte)40;
	
	public SM_PONG(){
		
	}
	
	public SM_PONG(byte[] data) throws IOException{
		this.data = data.clone();		
	}
	
	@Override
	public byte getOpCode() {
		// TODO Auto-generated method stub
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
	
	@Override
	public String toString(){
		return "opCode="+String.valueOf(OPCODE)+" data="+data;
	}
	
}
