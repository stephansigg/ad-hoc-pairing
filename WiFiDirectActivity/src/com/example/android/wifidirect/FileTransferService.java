// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.android.wifidirect;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.authentication.ambientaudio.AmbientAudioClient;
import org.authentication.ambientaudio.AmbientAudioPairing;
import org.authentication.ambientaudio.OnAmbientAudioResultListener;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService implements OnAmbientAudioResultListener {

	private static int NOTIF_AUTH_STARTED = 1;
	private static int NOTIF_AUTH_SUCCESS = 2;
	private static int NOTIF_AUTH_FAILURE = 3;
	
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    
    private AmbientAudioClient ambientAudioClient = null;

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                
                Socket remote = new Socket(host, port, false);
                if(ambientAudioClient == null)
                	ambientAudioClient = new AmbientAudioClient(remote, context, this);

                // successful authentication
                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                DeviceDetailFragment.copyFile(is, stream);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

	@Override
	public void onSessionKeyGeneratedSuccess(byte[] key, Socket remote) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionKeyGeneratedFailure(Socket remote, Exception e) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Stops the authentication client
	 */
	public void stopClient() {
		try {
			closeAmbientAudio();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Notify user that authentication started
	 */
	public void notifyAuthenticationStart() {
		Notification.Builder mBuilder =
	        new Notification.Builder(getApplicationContext())
	        .setSmallIcon(android.R.drawable.ic_dialog_info)
	        .setContentTitle("Authentication started...")
	        .setAutoCancel(true)
	        .setTicker("Authentication started ...")
	        .setContentText("This process usually takes about a minute");
		
		// Publish notification.
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(
					NOTIF_AUTH_STARTED, mBuilder.getNotification());		
	}
	
	/**
	 * Stop the ambient audio server and the ambient audio client
	 */
	private void closeAmbientAudio() {
		if (ambientAudioClient != null) {
			ambientAudioClient.finish();
			ambientAudioClient = null;
		}		
	}
}
