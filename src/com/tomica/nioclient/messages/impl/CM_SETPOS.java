package com.tomica.nioclient.messages.impl;

import java.nio.ByteBuffer;

import com.tomica.nioclient.messages.ClientMessage;
import com.tomica.nioserver.messages.Message;

public class CM_SETPOS extends ClientMessage{
	public static final byte OPCODE = (byte)25;	
	private long x;
	private float y;
	private int z;
	private byte flag;
	private short w;
	private double q;
	private String str;
	private char c;
		
	public CM_SETPOS(){
		
	}

	public CM_SETPOS(long x, float y, int z, byte flag, short w, double q, String str, char c) {		
		this.x = x;
		this.y = y;
		this.z=z;
		this.flag=flag;
		this.w=w;
		this.q=q;
		this.str=str;
		this.c=c;
		
		putLong(x);
		putFloat(y);
		putInt(z);
		putByte(flag);
		putShort(w);
		putDouble(q);
		putString(str);
		putChar(c);
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

	@Override
	public String toString(){
		return "opCode="+String.valueOf(OPCODE)+" x="+String.valueOf(x)+",y="+String.valueOf(y)+",z="+String.valueOf(z);
	}
}


