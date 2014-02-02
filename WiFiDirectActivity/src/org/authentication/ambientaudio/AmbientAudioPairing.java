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
import java.net.Socket;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.RemoteException;
import android.util.Log;

/**
 * Class for managing the ambient-audio-authentication method
 * @author Florian Schweitzer
 *
 */
public class AmbientAudioPairing implements OnAmbientAudioResultListener {
	
	private static int NOTIF_AUTH_STARTED = 1;
	private static int NOTIF_AUTH_SUCCESS = 2;
	private static int NOTIF_AUTH_FAILURE = 3;
	
	public static NotificationManager mNotificationManager;
	
	public static Context mContext;
	
	/**
	 * The single AmbientAudioPairing instance
	 */
	private static AmbientAudioPairing instance = null;
	
	/**
	 * The listener for the ambient audio result
	 */
	private static OnAmbientAudioResultListener ambientAudioResultListener = null;
	
	/**
	 * The ambient audio server object
	 */
	private static AmbientAudioServer ambientAudioServer = null;
	
	/**
	 * The ambient audio client object
	 */
	private static AmbientAudioClient ambientAudioClient = null;
	
	/**
	 * Gets a single instance of the Ambient Audio Pairing -object. Starts listening for
	 * incoming connections.
	 * 
	 * @return AmbientAudioPairing	The single AmbientAudioPairing instance
	 * @throws IOException
	 */
	public static AmbientAudioPairing getInstance(Context context, NotificationManager manager) throws IOException {
		if (AmbientAudioPairing.instance == null) {
			AmbientAudioPairing.instance = new AmbientAudioPairing();
			AmbientAudioPairing.mContext = context;
			AmbientAudioPairing.mNotificationManager = manager;
		}
		return AmbientAudioPairing.instance;
	}
	
	/**
	 * Stops the authentication server
	 */
	public static void stopServer() {
		try {
			closeAmbientAudio();
			
			if (instance != null) {
				instance = null;
				ambientAudioResultListener = null;	
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static AmbientAudioServer createAmbientAudioServer(Socket remote) {
		ambientAudioServer = new AmbientAudioServer(remote, mContext, getAmbientAudioResultListenerInstance());
		return ambientAudioServer;
	}
	
	/**
	 * Creates the ambient audio client object
	 * @param remote	The remote connection
	 */
	public static AmbientAudioClient createAmbientAudioClient(Socket remote) {
		ambientAudioClient = new AmbientAudioClient(remote, mContext, getAmbientAudioResultListenerInstance());
		return ambientAudioClient;
	}
	
	/**
	 * Notify user that authentication started
	 */
	public static void notifyAuthenticationStart() {
		Notification.Builder mBuilder =
	        new Notification.Builder(mContext)
	        .setSmallIcon(android.R.drawable.ic_dialog_info)
	        .setContentTitle("Authentication started...")
	        .setAutoCancel(true)
	        .setTicker("Authentication started ...")
	        .setContentText("This process usually takes about a minute");
		
		// Publish notification.
		mNotificationManager.notify(
					NOTIF_AUTH_STARTED, mBuilder.getNotification());		
	}
	
	/**
	 * Stop the ambient audio server and the ambient audio client
	 */
	private static void closeAmbientAudio() {
		if (ambientAudioClient != null) {
			ambientAudioClient.finish();
			ambientAudioClient = null;
		}		
		
		if (ambientAudioServer != null) {
			ambientAudioServer.finish();
			ambientAudioServer = null;
		}
	}
	
	/**
	 * Listener for getting informed of the authentication result
	 * @return
	 */
	public static OnAmbientAudioResultListener getAmbientAudioResultListenerInstance() {
		if (ambientAudioResultListener == null) {
			ambientAudioResultListener = new AmbientAudioPairing();
		}
		return ambientAudioResultListener;
	}

	@Override
	public synchronized void onSessionKeyGeneratedSuccess(byte[] key, Socket remote) {
		Log.i(this.toString(),"entered onSessionKeyGeneratedSuccess");	
		
		mNotificationManager.cancel(NOTIF_AUTH_STARTED);
		
		Notification.Builder mBuilder =
	        new Notification.Builder(mContext)
	        .setSmallIcon(android.R.drawable.ic_dialog_info)
	        .setContentTitle("Authentication succeed!")
	        .setAutoCancel(true)
	        .setTicker("Authentication successful!")
	        .setContentText("Successfully authenticated with ambient audio!");
		
		// Publish notification.
		mNotificationManager.notify(
					NOTIF_AUTH_SUCCESS, mBuilder.getNotification());
		
		//create SecureChannel
//		try {
//			if (key != null && remote != null) {
//				SecureChannel secChannel = new SecureChannel(remote);
//				secChannel.setSessionKey(key);
//				Client client = IConnectionType.getClientByRemote(remote);
//				
//				if (client != null) {
//					Log.i(this.toString(),"client != null, publishing secure channel");
//					client.setSecureChannel(secChannel);
//				} else {
//					Log.i(this.toString(),"client is null, no secure channel published");
//				}
//			}			
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}				
	}

	@Override
	public synchronized void onSessionKeyGeneratedFailure(
			Socket remote, Exception e) {
		Log.e(this.toString(), "entered onSessionKeyGeneratedFailure", e);
		
		mNotificationManager.cancel(NOTIF_AUTH_STARTED);		
		
		//Failure notification
		Notification.Builder mBuilder =
	        new Notification.Builder(mContext)
	        .setSmallIcon(android.R.drawable.ic_dialog_alert)
	        .setContentTitle("Authentication failed!")
	        .setAutoCancel(true)
	        .setTicker("Authentication failed!")
	        .setContentText("Failure during authentication with ambient audio!");	
		
		// Publish notification.
		mNotificationManager.notify(
					NOTIF_AUTH_FAILURE, mBuilder.getNotification());
	}
}
