/* Copyright Florian Schweitzer, An Huynh
 * File created 04.12.2012
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */
package org.authentication.ambientaudio;

import java.net.Socket;
import java.nio.ByteBuffer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Class to manage the client part of the
 * ambient audio pairing method
 * @author Florian Schweitzer
 *
 */
public class AmbientAudioClient extends Thread {
	
	/**
	 * All possible states of the client
	 */
	private static final byte STATE_NONE = 0;
	private static final byte STATE_SENT_CHALLENGE = 1;
	private static final byte STATE_RECEIVED_RECORD_COMMAND = 2;
	private static final byte STATE_STARTED_RECORDING = 3;
	private static final byte STATE_STARTED_FINGERPRINT_CALCULATION = 4;
	private static final byte STATE_STARTED_KEY_CALCULATION = 5;
	private static final byte STATE_FINGERPRINT_CALCULATION_FINISHED = 6;
	private static final byte STATE_KEY_GENERATED = 7;
	private static final byte STATE_CLIENT_FINISHED = 8;
	private static final byte STATE_SERVER_CLIENT_FINISHED = 9;
	private static final byte STATE_FAILURE_OCCURED = 10;
	
	/**
	 * The current state of the ambient audio client
	 */
	private byte currentState;
	
	/**
	 * The generated shared key
	 */
	private byte[] sharedKey;
	
	/**
	 * The listener for the ambient audio result
	 */
	private OnAmbientAudioResultListener ambientAudioResultListener = null;
    
    /**
	 * The communication thread of the client
	 */
	private CommunicationThread commThread;
	
	/**
	 * Flag if the server already finished the calculation
	 */
	private boolean isServerFinished = false;
	
	/**
	 * Flag if the client received the correction delta (for error correction)
	 */
	private boolean receivedCorrectionDelta = false;
	
	/**
	 * The received correction delta (for error correction)
	 */
	private byte[] correctionDelta = null;
	
	/**
	 * The remote connection to the server
	 */
	private Socket remoteConn;	

	/**
	 * The OpenUATService-Context
	 */
	private Context mContext;
	
	/**
	 * The timer for recording
	 */
	private Timer mTimer;
	
	/**
	 * AudioFingerprint-Object for calculating the audio fingerprint
	 */
	private AudioFingerprint mAudioFingerprint;
	
	/**
	 * ECCoder-Object for error correction
	 */
	private ECCoder mECCoder;
	
	/**
     * The client handler for message reading and writing
     */
	private static Handler clientHandler;
	
    /**
     * The handler thread for creating the client handler
     */
	private volatile Thread handlerThread = new Thread() {

		@Override
		public void run() {
			setName("AmbientAudioClientHandler-Thread");
				Looper.prepare();

				clientHandler = new Handler() {
					public void handleMessage(Message msg) {
						byte[] dataAll = null;
						byte[] data = null;

						switch (msg.what) {
						case CommunicationThread.MESSAGE_READ:

							dataAll = (byte[]) msg.obj;

							if (msg.arg1 <= 0) {
								Log.i(this.toString(),
										"received msg.arg=-1; return;");
								return;
							}
							data = new byte[msg.arg1];

							for (int i = 0; i < msg.arg1; i++) {
								data[i] = dataAll[i];
							}

							switch (data[0]) { // the command must always be at this position
							case AmbientAudioServer.RECORD_COMMAND:
								//retrieve recording start time
								byte[] buffer = new byte[8];
								for (int i = 0; i < 8; i++)
									buffer[i] = data[i + 1];
								long time = toLong(buffer);
								// Back up the recording atomic time
								mAudioFingerprint.setAtomicRecordingTime(time);
								// Convert to local time
								time -= mTimer.getTimeOffset();
								mAudioFingerprint
										.decideRecordingStartTime(time);

								Log.i(this.toString(), "record command: "
										+ time);
								Log.i(this.toString(),
										"planned recording start time client: "
												+ time);

								// Change state only after the recording start
								// time is set for mAudioFingerprint
								currentState = STATE_RECEIVED_RECORD_COMMAND;
								AmbientAudioPairing.notifyAuthenticationStart();
								Log.i(this.toString(),
										"client state switched to state received record command");

								// respond server with ACK
								byte[] ack = new byte[1];
								ack[0] = AmbientAudioServer.ACK;
								commThread.write(ack);
								break;
							case AmbientAudioServer.SERVER_IS_FINISHED:
								isServerFinished = true;
								break;
							case AmbientAudioServer.DELTA_INFO:
								// retrieve data out of received bytes array
								correctionDelta = new byte[data.length - 1]; //command byte is at this position

								for (int i = 0; i < correctionDelta.length; i++) {
									correctionDelta[i] = data[i + 1];
								}
								receivedCorrectionDelta = true;
								Log.i(this.toString(),
										"ambient audio client received correction delta");

								break;
							case AmbientAudioServer.SERVER_FAILED:
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
	
	/**
     * Constructor for the ambient audio client
     * @param remoteConn		connection to the remote device
     * @param keyGenListener	listener for the generated key
     */
	public AmbientAudioClient(Socket remote, OnAmbientAudioResultListener resultListener) {
		
		setName("AmbientAudioClient-Thread");
		currentState = STATE_NONE;		
		ambientAudioResultListener = resultListener;
		remoteConn = remote;
		
		Log.i(this.toString(),"in AmbientAudioClient-constructor");
		Log.i(this.toString(),remoteConn.toString());
		Log.i(this.toString(),"ambient audio result listener: " + ambientAudioResultListener.toString());
		
		//initialize AmbientAudio-related classes
		initialize();
		
		startHandlerThread();
		
		while (clientHandler == null) {};
		
		//start the client part
		start();
		
		Log.i(this.toString(),"ambientaudioclient is constructed");
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
	 * Stop all background threads
	 */
	public void stopThreads() {
		if (commThread != null) {
			commThread.finish();
		}
		
		if (clientHandler != null) {
			clientHandler.getLooper().quit();
			clientHandler = null;
		}
		handlerThread = null;
		
		Log.i(this.toString(), "all threads in AmbientAudioClient stopped");
	}
	
	@Override
	public void run() {		
		try {
			Looper.prepare();
			while (currentState != STATE_SERVER_CLIENT_FINISHED && currentState != STATE_FAILURE_OCCURED) {
				if (commThread == null) {
		        	commThread = new CommunicationThread(remoteConn,clientHandler);
		    		commThread.start();
		    		
		    		//send challenge to server
		    		sendAuthenticationChallengeToServer();
		        }
		        
		        switch (currentState) {
		        case STATE_RECEIVED_RECORD_COMMAND:
		        	startRecording();
		        	break;
		        case STATE_FINGERPRINT_CALCULATION_FINISHED:
		        	//if server alreday sent the correction delta, calculate the shared key
		        	if (receivedCorrectionDelta) {
		        		calculateSharedKey();	        		
		        	}
		        	break;
		        case STATE_KEY_GENERATED:
		        	sendFinishInformationToServer(); //inform server that client is finished
		        	break;
		        case STATE_CLIENT_FINISHED:
		        	if (isServerFinished) { //wait for server
		        		currentState = STATE_SERVER_CLIENT_FINISHED;
		        		Log.i(this.toString(),"client state switched to: state_server_client_finished");
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
	 * Sends a failure information to the server
	 */
	public void sendFailureInformation() {
		byte[] failInfo = new byte[1];
		failInfo[0] = AmbientAudioServer.CLIENT_FAILED;
		
		commThread.write(failInfo);
		currentState = STATE_FAILURE_OCCURED;
		Log.i(this.toString(),"client sent fail info");			
	}
	
	/**
	 * Start authenticating with sending a challenge
	 */
	public void sendAuthenticationChallengeToServer() {
		byte[] challenge = new byte[1];
		challenge[0] = AmbientAudioServer.CLIENT_CHALLENGE;
		
		commThread.write(challenge);
		currentState = STATE_SENT_CHALLENGE;
		Log.i(this.toString(),"client state switched to: STATE_SENT_CHALLENGE");		
	}
	
	/**
	 * Informs the server that the client finished key generation
	 */
	public void sendFinishInformationToServer() {
		byte[] finishMessage = new byte[1];
		finishMessage[0] = AmbientAudioServer.CLIENT_IS_FINISHED;
		
		commThread.write(finishMessage);
		currentState = STATE_CLIENT_FINISHED;	
		Log.i(this.toString(),"client state switched to: state_client_finished");
	}
	
	/**
	 * Starts audio-recording and the fingerprint calculation
	 */
	public void startRecording() {
		//start recording at the given time
		currentState = STATE_STARTED_RECORDING;
		Log.i(this.toString(),"client state switched to start recording");
		
		mAudioFingerprint.startRecording();		
		mAudioFingerprint.startMatchingPattern();
		
		//after recording process, start the fingerprint calculation thread	
		currentState = STATE_STARTED_FINGERPRINT_CALCULATION;
		Log.i(this.toString(),"client state switched to fingerprint calculation");
		
		mAudioFingerprint.startCalculatingFingerprint(mAudioFingerprint.getPatternMatchingShiftTime());
		
		currentState = STATE_FINGERPRINT_CALCULATION_FINISHED;
		Log.i(this.toString(),"client state switched to fingerprint calc finished");
		
		//Simulate calculation time ------------------
		/*try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		//----------------------------------------------
	}
	
	/**
	 * Calculates the shared key based on the correction delta (sent by the server)
	 */
	private void calculateSharedKey() {		
		//after that start the key calculation
		currentState = STATE_STARTED_KEY_CALCULATION;
		Log.i(this.toString(),"client state switched to key calculation");
		
		// Convert the fingerprint
		int shifttime = mAudioFingerprint.getPatternMatchingShiftTime();
		byte[][] fingerprint = mAudioFingerprint.getFingerprint(shifttime);
		byte[] codewordBytes = new byte[AudioFingerprint.fingerprintBits];
    	int line_leng = fingerprint[0].length;
    	for (int i=0; i<codewordBytes.length; i++)
    		codewordBytes[i] = fingerprint[i/line_leng][i%line_leng];
    	// Convert delta
		int[] deltaInt = new int[correctionDelta.length];
		for (int i=0; i<correctionDelta.length; i++) {
			deltaInt[i] = correctionDelta[i];
		}
		// Do decommit
    	int err;
    	err = mECCoder.decommit(codewordBytes, deltaInt);
    	if (err == -1) {
    		Log.e(this.toString(), "Error in mECCoder.decommit().");
    		sendFailureInformation();
    		return;
    	}
    	// Get key
    	int[] sharedKeyInt = mECCoder.getPlainWord();
		sharedKey = new byte[sharedKeyInt.length];
		for (int i=0; i<sharedKeyInt.length; i++) {
			sharedKey[i] = (byte) sharedKeyInt[i];
		}
    	
		currentState = STATE_KEY_GENERATED;		
		Log.i(this.toString(),"client state switched to key generated");
		
		if (sharedKey != null) {
			String s = "";
			for (int i = 0 ; i < sharedKey.length ; i++) {
				s += sharedKey[i] + ",";
			}
			Log.i(this.toString(),"shared key of client: " + s);
		}
	}

	/**
	 * Initializes the ambient audio related classes
	 */
	private void initialize() {
		// Initialize timer
		mTimer = new Timer(mContext);
		mTimer.start();
		// Initialize AudioFingerprint
		mAudioFingerprint = new AudioFingerprint(mContext);
		mAudioFingerprint.initialize();
		// RS
		int n = AudioFingerprint.fingerprintBits;
		int m = (int) Math.round(n - 2*AmbientAudioServer.DIFF_LIMIT*n);
		int symsize = 2;
		for (int i=0; i<ECCoder.defaultRSParamCount; i++)
			if (ECCoder.defaultRSParameters[i][3] > n) {
				symsize = i;
				break;
			}
		mECCoder = new ECCoder(n, m, symsize);
	}
	
	/**
	 * Helper method for converting a byte-Array to long
	 * @param bytearray		The byte-array to convert
	 * @return				The calculated long value
	 */
	private long toLong(byte[] bytearray) {
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer = (ByteBuffer) buffer.put(bytearray).position(0);
    	long l = buffer.getLong();
    	return l;
    }

	/**
	 * Finishes the ambient-audio-client thread
	 */
	public void finish() {
		try {
			Log.i(this.toString(),"in finish of AmbientAudioClient");
			
			if (mTimer != null)
				mTimer.stop();
			
			stopThreads();

			if (currentState == STATE_SERVER_CLIENT_FINISHED) {
				byte[] key;
				key = CipherUtils.HashSHA256(sharedKey);
				ambientAudioResultListener.onSessionKeyGeneratedSuccess(key,
						remoteConn);
				
				Log.i(this.toString(),"after onSessionKeyGeneratedSuccess in AmbientAudioClient");
			} else {
				ambientAudioResultListener.onSessionKeyGeneratedFailure(
						remoteConn, new Exception(
								"Ambient-Audio-Calculation failed"));			
				
				Log.i(this.toString(),"after onSessionKeyGeneratedFailure in AmbientAudioClient");
			}

		} catch (Exception e) {
			ambientAudioResultListener.onSessionKeyGeneratedFailure(
					remoteConn, e);
			Log.i(this.toString(),"after onSessionKeyGeneratedFailure in AmbientAudioClient");
		}
	}
}
