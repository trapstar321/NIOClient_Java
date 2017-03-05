package com.tomica.nioclient;

public class IntentChangeRequest {
	private int ops;
	
	public IntentChangeRequest(int ops){
		this.ops=ops;
	}
	
	public int getIntent(){
		return ops;
	}
}
