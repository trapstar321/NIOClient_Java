package com.tomica.nioclient.exceptions;

public class ClientNotConnectedException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ClientNotConnectedException(String message) {
        super(message);
    }
}
