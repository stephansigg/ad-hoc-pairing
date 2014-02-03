/* Copyright Florian Schweitzer, An Huynh
 * File created 04.12.2012
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package com.example.android.wifidirect.test;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.authentication.ambientaudio.CipherUtils;
import org.authentication.ambientaudio.CommunicationThread;
import org.authentication.ambientaudio.ECCoder;
import org.authentication.ambientaudio.OnAmbientAudioResultListener;
import org.authentication.ambientaudio.Timer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Class manages the server part of the ambient audio pairing
 * @author Florian Schweitzer
 *
 */
public class AmbientAudioServerTest extends Thread {
	
	/**
	 * All possible states of the server
	 */
	private static final byte STATE_NONE = 0;
	private static final byte STATE_CHALLENGE_ACCEPTED = 1;
	private static final byte STATE_SENT_RECORD_COMMAND = 2;
	private static final byte STATE_CLIENT_ACCEPTED_RECORD_COMMAND = 3;
	private static final byte STATE_STARTED_RECORDING = 4;
	private static final byte STATE_STARTED_FINGERPRINT_CALCULATION = 5;
	private static final byte STATE_STARTED_DELTA_CALCULATION = 6;
	private static final byte STATE_DELTA_CALCULATION_FINISHED = 7;
	private static final byte STATE_DELTA_SENT = 8;
	private static final byte STATE_STARTED_KEY_CALCULATION = 9;
	private static final byte STATE_KEY_GENERATED = 10;
	private static final byte STATE_SERVER_FINISHED = 11;
	private static final byte STATE_SERVER_CLIENT_FINISHED = 12;
	private static final byte STATE_FAILURE_OCCURED = 13;
	
	 //Commands for communication between server and client
    public static final byte RECORD_COMMAND = 1;
    public static final byte ACK = 2;
    public static final byte DELTA_INFO = 3;
    public static final byte SERVER_IS_FINISHED = 4;
    public static final byte CLIENT_IS_FINISHED = 5;
    public static final byte CLIENT_CHALLENGE = 6;
    public static final byte CLIENT_FAILED = 7;
    public static final byte SERVER_FAILED = 8;
	
	/**
	 * Flag if the client already finished the calculation
	 */
	private boolean isClientFinished = false;
	
	/**
	 * The current state of the ambient audio server
	 */
	private byte currentState;
	
	/**
	 * The listener for key informing
	 */
	private OnAmbientAudioResultListener ambientAudioResultListener = null;
    
    /**
	 * The generated shared key
	 */
	private byte[] sharedKey;
	
	/**
	 * The remote connection to the client
	 */
    private Socket remoteConn;
	
	/**
	 * The communication thread of the server
	 */
	private CommunicationThread commThread;	
	
	
	/**
	 * The maximum limit of decodability between the calculated audio fingerprints
	 */
    public static final double DIFF_LIMIT = 0.49;
    
	/**
	 * The OpenUATService-object
	 */
	private Context mContext;
	
	/**
	 * The timer for start recording
	 */
	private Timer mTimer;
	
	/**
	 * The audio fingerprint object for calculating the audio fingerprint
	 */
	private AudioFingerprintTest mAudioFingerprint;
	
	/**
	 * The ECCoder-object for error correction
	 */
	private ECCoder mECCoder;
    
    /**
     * The server handler for message reading and writing
     */
    private static Handler serverHandler;
    
    /**
     * The handler thread for creating the handler
     */
    private volatile Thread handlerThread = new Thread() {
    	
		@Override
		public void run() {
			setName("AmbientAudioServerHandler-Thread");

				Looper.prepare();

				serverHandler = new Handler() {
					public void handleMessage(Message msg) {
						byte[] dataAll = null;
						byte[] data = null;

						switch (msg.what) {
						case CommunicationThread.MESSAGE_READ:
							dataAll = (byte[]) msg.obj;

							if (msg.arg1 <= 0) {
								return;
							}

							data = new byte[msg.arg1];

							for (int i = 0; i < msg.arg1; i++) {
								data[i] = dataAll[i];
							}

							switch (data[0]) { // the command should always be
												// in
												// data[0]
							case AmbientAudioServerTest.CLIENT_CHALLENGE:
								currentState = STATE_CHALLENGE_ACCEPTED;
								//AmbientAudioPairing.notifyAuthenticationStart();
								Log.i(this.toString(),
										"server state switched to: STATE_CHALLENGE_ACCEPTED");
								break;
							case AmbientAudioServerTest.ACK:
								currentState = STATE_CLIENT_ACCEPTED_RECORD_COMMAND;
								Log.i(this.toString(),
										"server state switched to: client accepted record command");
								break;
							case AmbientAudioServerTest.CLIENT_IS_FINISHED:
								isClientFinished = true;
								Log.i(this.toString(),
										"server received that client is finished");
								break;
							case AmbientAudioServerTest.CLIENT_FAILED:
								currentState = STATE_FAILURE_OCCURED;
								break;
							}

							break;
						}
					}
				};
				Looper.loop();
		}

	};	
	
	/*
	 * For test
	 */
	public AmbientAudioServerTest(Context context) {		
		currentState = STATE_NONE;
		//remoteConn = remote;
		mContext = context;
		//ambientAudioResultListener = resultListener;
		setName("AmbientAudioServer-Thread");
		
		Log.i(this.toString(),"in AmbientAudioServer-constructor");
		//Log.i(this.toString(),remoteConn.toString());
		//Log.i(this.toString(),"ambient audio result listener: " + ambientAudioResultListener.toString());
		
		//initialize AmbientAudio-related classes
		initialize();
			
		Log.i(this.toString(),"ambientaudioserver is constructed");
	}
	
	/**
     * Constructor for the ambient audio server
     * @param remoteConn		connection to the remote device
     * @param keyGenListener	listener for the generated key
     */
	public AmbientAudioServerTest(Socket remote, Context context, OnAmbientAudioResultListener resultListener) {		
		currentState = STATE_NONE;
		remoteConn = remote;
		mContext = context;
		ambientAudioResultListener = resultListener;
		setName("AmbientAudioServer-Thread");
		
		Log.i(this.toString(),"in AmbientAudioServer-constructor");
		Log.i(this.toString(),remoteConn.toString());
		Log.i(this.toString(),"ambient audio result listener: " + ambientAudioResultListener.toString());
		
		//initialize AmbientAudio-related classes
		initialize();
		
		startHandlerThread();
		
		while (serverHandler == null) {};
		
		//start the server part
		this.start();
		
		Log.i(this.toString(),"ambientaudioserver is constructed");
	}
	
	/**
	 * Sends a failure information to the client
	 */
	public void sendFailureInformation() {
		byte[] failInfo = new byte[1];
		failInfo[0] = AmbientAudioServerTest.CLIENT_FAILED;
		
		commThread.write(failInfo);
		currentState = STATE_FAILURE_OCCURED;
		Log.i(this.toString(),"client sent fail info");			
	}
	
	/**
	 * Starts the handler thread
	 */
	public void startHandlerThread() {
		if (handlerThread != null) {
			handlerThread.start();
		}
	}
	
	/**
	 * Stops the background threads
	 */
	public void stopThreads() {
		if (commThread != null) {
			commThread.finish();
			//commThread = null;			
		}
		
		
		if (serverHandler != null) {
			serverHandler.getLooper().quit();
			serverHandler = null;
		}
		handlerThread = null;
		
		Log.i(this.toString(), "all threads in AmbientAudioServer stopped");
	}
	
	public void run() {
		try {
			Looper.prepare();
			while (currentState != STATE_SERVER_CLIENT_FINISHED && currentState != STATE_FAILURE_OCCURED) {
				if (commThread == null) {
		        	commThread = new CommunicationThread(remoteConn,serverHandler);
		    		commThread.start();
		        }
		        
		        switch (currentState) {
		        case STATE_CHALLENGE_ACCEPTED:
		        	requestClientForRecording();
		        	break;
		        case STATE_CLIENT_ACCEPTED_RECORD_COMMAND:
		        	startRecordingAndCalculation();
		        	break;
		        case STATE_KEY_GENERATED:
		        	sendFinishInformationToClient(); //inform client that server is finished
		        	break;
		        case STATE_SERVER_FINISHED:
		    		if (isClientFinished) { //wait for client
		    			currentState = STATE_SERVER_CLIENT_FINISHED;	
		    			Log.i(this.toString(),"server state switched to: state_server_client_finished");
		    			finish();
		    		}
		    		break;
		        }
			}			
		} catch (Exception e) {
			sendFailureInformation();
		}		
		finish();
	}
	
	/**
	 * Sends the client the recording command
	 */
	public void requestClientForRecording() {
		// Get Recording Start Time as a byte array
		byte[] d = getRecordingStartTime();
		// Put RECORD_COMMAND on array's head
    	int n = d.length;
    	byte[] recordMessage = new byte[n+1];
    	recordMessage[0] = RECORD_COMMAND;
    	for (int i=1; i<n+1; i++)
    		recordMessage[i] = d[i-1];
    	
		// Send request
    	if (commThread != null) {
    		commThread.write(recordMessage);
    		// Change state
    		currentState = STATE_SENT_RECORD_COMMAND;
    		Log.i(this.toString(),"server state switched to: sent record command");    		
    	}
	}
	
	/**
	 * Informs the client that the server finished key generation
	 */
	public void sendFinishInformationToClient() {
		byte[] finishMessage = new byte[1];
		finishMessage[0] = SERVER_IS_FINISHED;
		
		if (commThread != null) {
			commThread.write(finishMessage);
			currentState = STATE_SERVER_FINISHED;	
			Log.i(this.toString(),"server state switched to: state_server_finished");			
		}
	}
	
	/**
	 * Starts the audio recording and key calculation
	 */
	public void startRecordingAndCalculation() {
		//start recording at the given time
		currentState = STATE_STARTED_RECORDING;
		Log.i(this.toString(),"server state switched to start recording");
		
		mAudioFingerprint.startRecording();
		mAudioFingerprint.startMatchingPattern();
		int shifttime = mAudioFingerprint.getPatternMatchingShiftTime();
		
		//after recording process, start the fingerprint calculation thread	
		currentState = STATE_STARTED_FINGERPRINT_CALCULATION;
		Log.i(this.toString(),"server state switched to fingerprint calculation");
		
		mAudioFingerprint.startCalculatingFingerprint(shifttime);
		
		//after fingerprint calculation -> generate delta and send to client
		currentState = STATE_STARTED_DELTA_CALCULATION;
		Log.i(this.toString(),"server state switched to delta calculation");

		// Convert the fingerprint
		byte[][] fingerprint = mAudioFingerprint.getFingerprint(shifttime);
		byte[] codewordBytes = new byte[AudioFingerprintTest.fingerprintBits];
		int line_leng = fingerprint[0].length;
		for (int i=0; i<codewordBytes.length; i++)
			codewordBytes[i] = fingerprint[i/line_leng][i%line_leng];
		// Do commit
		int err = mECCoder.commit(codewordBytes);
    	if (err == -1) {
    		Log.e(this.toString(), "There is not recorded audio data yet.");
    		return ;
    	}
    	// Get delta
		int[] deltaInt = mECCoder.getDelta();		
		byte[] delta = new byte[deltaInt.length+1]; //temporary byte-array (should represent the calculated delta-array)
		delta[0] = DELTA_INFO;
		for (int i=0; i<deltaInt.length; i++) {
			delta[i+1] = (byte) deltaInt[i];
		}
		
		//after generating delta -> send delta to client		
		currentState = STATE_DELTA_CALCULATION_FINISHED;
		Log.i(this.toString(),"server state switched to delta_calculation_finished");
		
		commThread.write(delta); //send the correction delta to the client
		
		currentState = STATE_DELTA_SENT;
		Log.i(this.toString(),"server state switched to state_delta_sent");
		
		//after key generation set the "shared key" instance variable and the current state
		currentState = STATE_STARTED_KEY_CALCULATION;
		Log.i(this.toString(),"server state switched to key calculation");

		int[] sharedKeyInt = mECCoder.getPlainWord();
		sharedKey = new byte[sharedKeyInt.length];
		for (int i=0; i<sharedKeyInt.length; i++) {
			sharedKey[i] = (byte) sharedKeyInt[i];
		}
		
		currentState = STATE_KEY_GENERATED;
		Log.i(this.toString(),"server state switched to key generated");
		
		if (sharedKey != null) {
			String s = "";
			for (int i = 0 ; i < sharedKey.length ; i++) {
				s += sharedKey[i] + ",";
			}
			Log.i(this.toString(),"shared key of server: " + s);
		}
		
		//Simulate calculation time ------------------
		/*try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		//----------------------------------------------
	}
	
	/**
	 * For test
	 */
	public void startRecordingAndCalculation(int test) {
		//start recording at the given time
		currentState = STATE_STARTED_RECORDING;
		Log.i(this.toString(),"server state switched to start recording");
		
		mAudioFingerprint.startRecording();
		mAudioFingerprint.startMatchingPattern();
		int shifttime = mAudioFingerprint.getPatternMatchingShiftTime();
		
		//after recording process, start the fingerprint calculation thread	
		currentState = STATE_STARTED_FINGERPRINT_CALCULATION;
		Log.i(this.toString(),"server state switched to fingerprint calculation");
		
		mAudioFingerprint.startCalculatingFingerprint(shifttime);
		
		//after fingerprint calculation -> generate delta and send to client
		currentState = STATE_STARTED_DELTA_CALCULATION;
		Log.i(this.toString(),"server state switched to delta calculation");

		// Convert the fingerprint
		byte[][] fingerprint = mAudioFingerprint.getFingerprint(shifttime);
		byte[] codewordBytes = new byte[AudioFingerprintTest.fingerprintBits];
		int line_leng = fingerprint[0].length;
		for (int i=0; i<codewordBytes.length; i++)
			codewordBytes[i] = fingerprint[i/line_leng][i%line_leng];
		// Do commit
		int err = mECCoder.commit(codewordBytes);
    	if (err == -1) {
    		Log.e(this.toString(), "There is not recorded audio data yet.");
    		return ;
    	}
    	// Get delta
		int[] deltaInt = mECCoder.getDelta();		
		byte[] delta = new byte[deltaInt.length+1]; //temporary byte-array (should represent the calculated delta-array)
		delta[0] = DELTA_INFO;
		for (int i=0; i<deltaInt.length; i++) {
			delta[i+1] = (byte) deltaInt[i];
		}
		
		//after generating delta -> send delta to client		
		currentState = STATE_DELTA_CALCULATION_FINISHED;
		Log.i(this.toString(),"server state switched to delta_calculation_finished");
		
		//commThread.write(delta); //send the correction delta to the client
		
		currentState = STATE_DELTA_SENT;
		Log.i(this.toString(),"server state switched to state_delta_sent");
		
		//after key generation set the "shared key" instance variable and the current state
		currentState = STATE_STARTED_KEY_CALCULATION;
		Log.i(this.toString(),"server state switched to key calculation");

		int[] sharedKeyInt = mECCoder.getPlainWord();
		sharedKey = new byte[sharedKeyInt.length];
		for (int i=0; i<sharedKeyInt.length; i++) {
			sharedKey[i] = (byte) sharedKeyInt[i];
		}
		
		currentState = STATE_KEY_GENERATED;
		Log.i(this.toString(),"server state switched to key generated");
		
		if (sharedKey != null) {
			String s = "";
			for (int i = 0 ; i < sharedKey.length ; i++) {
				s += sharedKey[i] + ",";
			}
			Log.i(this.toString(),"shared key of server: " + s);
		}
		
		//Simulate calculation time ------------------
		/*try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		//----------------------------------------------
	}
	
	/**
	 * Initializes the ambient audio related classes
	 */
	private void initialize() {
		// Initialize timer
		mTimer = new Timer(mContext);
		mTimer.start();
		// Initialize AudioFingerprint
    	mAudioFingerprint = new AudioFingerprintTest(mContext);
    	mAudioFingerprint.initialize();
    	// RS
    	int n = AudioFingerprintTest.fingerprintBits;
		int m = (int) Math.round(n - 2*DIFF_LIMIT*n);
    	int symsize = 2;
    	for (int i=0; i<ECCoder.defaultRSParamCount; i++)
			if (ECCoder.defaultRSParameters[i][3] > n) {
				symsize = i;
				break;
			}
    	mECCoder = new ECCoder(n, m, symsize);
	}

	/**
	 * Converts a long to a byte-Array
	 * @param l		The long to convert
	 * @return		The calculted byte-Array
	 */
	private byte[] toBytes(long l) {
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.putLong(l);
    	return buffer.array();
    }

	/**
	 * Gets the recording start-time
	 * @return		The recording start as a byte array
	 */
	private byte[] getRecordingStartTime() {
		// Order this device to get sample and receive back the sampling time-point
		long starttime = mAudioFingerprint.decideRecordingStartTime(0);
		// Convert this time-point into the atomic time-point
		starttime += mTimer.getTimeOffset();
		// Back up the recording atomic time
		mAudioFingerprint.setAtomicRecordingTime(starttime);

		Log.i(this.toString(),"planned recording start time server: " + starttime);
		return toBytes(starttime);
	}
	
	/**
	 * Finishes the ambient audio server thread
	 */
	public void finish() {
		try {
			
			Log.i(this.toString(),"in finish of AmbientAudioServer");
			if (mTimer != null)
				mTimer.stop();
			
			stopThreads();

			if (currentState == STATE_SERVER_CLIENT_FINISHED) {
				//byte[] key;
				//key = CipherUtils.HashSHA256(sharedKey);
				ambientAudioResultListener.onSessionKeyGeneratedSuccess(sharedKey,
						remoteConn);
				Log.i(this.toString(),"after onSessionKeyGeneratedSuccess in AmbientAudioServer");
			} else {
				ambientAudioResultListener.onSessionKeyGeneratedFailure(
						remoteConn, new Exception(
								"Ambient-Audio-Calculation failed"));	
				
				Log.i(this.toString(),"after onSessionKeyGeneratedFailure in AmbientAudioServer");
			}

		} catch (Exception e) {
			ambientAudioResultListener.onSessionKeyGeneratedFailure(
					remoteConn, e);
			
			Log.i(this.toString(),"after onSessionKeyGeneratedFailure in AmbientAudioServer");
		}
	}
	

	public void encrypt(String fileIn, String fileOut) {
		try {
			CipherUtils.encrypt(fileIn, fileOut, sharedKey);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void decrypt(String fileIn, String fileOut) {
		try {
			CipherUtils.decrypt(fileIn, fileOut, sharedKey);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
