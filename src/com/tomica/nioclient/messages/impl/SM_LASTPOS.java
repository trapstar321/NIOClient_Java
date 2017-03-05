package com.tomica.nioclient.messages.impl;

import java.nio.ByteBuffer;

import com.tomica.nioclient.messages.ServerMessage;
import com.tomica.nioserver.messages.Message;;

public class SM_LASTPOS extends ServerMessage{
	public static final byte OPCODE=(byte)26;	
	private long x;		
	private float y;
	private int z;
	private byte flag;
	private short w;
	private double q;
	private char c;
	private String str;

	public SM_LASTPOS(){
		
	}
	
	public SM_LASTPOS(byte[] data) {
		this.data = data.clone();
		x=getLong();
		y=getFloat();
		z=getInt();
		flag=getByte();
		w=getShort();
		q=getDouble();
		str=getString();
		c=getChar();		
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
	
	public long getX(){return x;}
	
	public float getY(){return y;}
	
	public int getZ(){return z;}
	
	public byte getFlag(){return flag;}
	
	public short getW(){return w;}
	
	public double getQ(){return q;}
	
	
	@Override
	public byte getOpCode() {
		return OPCODE;
	}
	
	@Override
	public String toString(){		
		return "opCode="+String.valueOf(OPCODE)+" x="+String.valueOf(x)+
				",y="+String.valueOf(y)+
				",z="+String.valueOf(z)+
				",w="+String.valueOf(w)+
				",q="+String.valueOf(q)+
				",c="+String.valueOf(c)+
				",str="+str+
				",flag="+flag;		
	}

}
