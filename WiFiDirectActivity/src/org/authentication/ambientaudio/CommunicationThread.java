/* Copyright Florian Schweitzer
 * File created 04.12.2012
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package org.authentication.ambientaudio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;

import com.example.android.wifidirect.SimpleLog;

import android.os.Handler;
import android.util.Log;

/**
 * This thread runs during a connection with a remote device.
 * It can read messages from an input stream and write
 * message to an output stream
 */
public class CommunicationThread extends Thread {
	
	//Message types for the handler
    public static final byte MESSAGE_READ = 1;
    public static final byte MESSAGE_WRITE = 2;
    
    /**
     * The last byte to inform partner for closing
     */
    public static final byte CLOSING_BYTE = 111;
	
	/**
	 * The input stream of the server
	 */
    private InputStream inputStream;
    
    /**
	 * The output stream of the server
	 */
    private OutputStream outputStream;
    
    /**
     * The handler to inform
     */
    private Handler mHandler = null;
    
    /**
     * Flag, if closing byte was sent
     */
    private boolean sentClosingByte = false;
    
    /**
     * Flag, if closing byte is already received
     */
    private boolean receivedClosingByte = false;

    /**
     * Constructor
     * @param remoteConn	The connection to the remote device
     * @param handler		The handler to inform
     */
    public CommunicationThread(Socket remoteConn, Handler handler) {

        try {
        	if (remoteConn != null) {
        		setName("CommunicationThread-" + remoteConn.toString());
    			outputStream = remoteConn.getOutputStream();
    			inputStream = remoteConn.getInputStream();
    			mHandler = handler;
        	}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @Override
    public void run() {
    	while (true) {
    		
    		if (sentClosingByte && receivedClosingByte) {
    			break;
    		}
			
			Log.i(this.toString(), "in while loop of CommThread");
			SimpleLog.appendLog(this.toString() + " in while loop of CommThread");
	        byte[] buffer = new byte[1024];
	        int bytes;

	        // Keep listening to the InputStream while connected
	        if (inputStream != null && mHandler != null) {
	            try {
	            	
	            	if (!receivedClosingByte) {
	            		
	            		 // Read from the InputStream
		                bytes = inputStream.read(buffer);
		                
		                if (buffer != null && buffer.length > 0) {
		                	if (buffer[0] == CLOSING_BYTE) {
		                		Log.i(this.toString(),"CommThread received closing byte");
		                		SimpleLog.appendLog(this.toString() + " Communication Thread received closing byte");
		                		receivedClosingByte = true;
		                	} else {
		                		if (inputStream != null && mHandler != null) {
				                	mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();	                	
				                }	       		                		
		                	}
		                }	                         
	            		
	            	}
	            } catch (IOException e) {
	                Log.e(this.toString(), "disconnected", e);
	                break;
	            }
	        }
		}
    	
    	Log.i(this.toString(),"after while() CommThread --> closing");
    	SimpleLog.appendLog(this.toString() + " after while() CommThread --> closing");
    	Log.i(this.toString(), "value of sentClosingByte: " + sentClosingByte);
    	SimpleLog.appendLog(this.toString() + " value of sentClosingByte: " + sentClosingByte);
    	Log.i(this.toString(), "value of receivedClosingByte: " + receivedClosingByte);
    	SimpleLog.appendLog(this.toString() + " value of receivedClosingByte: " + receivedClosingByte);
    }

    /**
     * Write to the connected OutStream.
     * @param buffer  The bytes to write
     */
    public void write(byte[] buffer) {
        try {
        	if (outputStream != null && mHandler != null) {
        		outputStream.write(buffer);
        		
        		outputStream.flush();
        		
        		if (buffer != null && buffer.length > 0) {
                	if (buffer[0] == CLOSING_BYTE) {
                		sentClosingByte = true;
                		Log.i(this.toString(),"CommThread sent closing byte");
                	} else {
                		// Share the sent message back
                        mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                        Log.i(this.toString(),"commthread_write");                		
                	}
                }	               	
        	}
        } catch (IOException e) {
            Log.e(this.toString(), "Exception during write", e);
        }
    }
    
	/**
	 * Finishes the communication thread
	 */
	public void finish() {
		Log.i(this.toString(),"in CommThread finish()");
		
		byte[] closing = { CLOSING_BYTE };
		write(closing);
		Log.i(this.toString(),"after write closing byte");
		
//		try {
//			if(inputStream != null)
//				inputStream.close();
//			if(outputStream != null)
//				outputStream.close();
//			SimpleLog.appendLog(this.toString() + " Communication Thread finished successfully");
//		}
//		catch(IOException e) {
//			SimpleLog.appendLog("Communication Thread " + e.getMessage());
//		}
	}
}