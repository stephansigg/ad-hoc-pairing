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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.authentication.ambientaudio.AmbientAudioClient;
import org.authentication.ambientaudio.AmbientAudioPairing;
import org.authentication.ambientaudio.CipherUtils;
import org.authentication.ambientaudio.OnAmbientAudioResultListener;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService implements OnAmbientAudioResultListener {

	private static int NOTIF_AUTH_STARTED = 1;
	private static int NOTIF_AUTH_SUCCESS = 2;
	private static int NOTIF_AUTH_FAILURE = 3;
	//private static int NOTIF_DECRYPT_SUCCESS = 4;
	
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    
    private AmbientAudioClient ambientAudioClient = null;
    private Context context = null;
    private NotificationManager mNotificationManager = null;
    //private Socket serverSocket = null;
    private Socket socket = null;
    private String fileUri = null;

    public FileTransferService(String name) {
        super(name);
        
        context = getApplicationContext();
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    public FileTransferService() {
        super("FileTransferService");
        
        context = getApplicationContext();
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                
                //serverSocket = new Socket(host, port, false);
                if(ambientAudioClient == null)
                	ambientAudioClient = new AmbientAudioClient(socket, context, this);
                notifyAuthenticationStart();
                // successful authentication
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                //
            }

        }
    }

	@Override
	public void onSessionKeyGeneratedSuccess(byte[] key, Socket remote) {
		Log.i(this.toString(),"entered onSessionKeyGeneratedSuccess");	
		
		mNotificationManager.cancel(NOTIF_AUTH_STARTED);
		
		Notification.Builder mBuilder =
	        new Notification.Builder(context)
	        .setSmallIcon(android.R.drawable.ic_dialog_info)
	        .setContentTitle("Authentication succeed!")
	        .setAutoCancel(true)
	        .setTicker("Authentication successful!")
	        .setContentText("Successfully authenticated with ambient audio!");
		
		// Publish notification.
		mNotificationManager.notify(
					NOTIF_AUTH_SUCCESS, mBuilder.getNotification());
		
		try {
			if(fileUri != null) {
				Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
				
				File f = new File(fileUri);
				String encryptedFileUri = f.getParent() + "/ENCRYPTED_" + f.getName();
				CipherUtils.encrypt(fileUri, encryptedFileUri, key);
				fileUri = encryptedFileUri;
				
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
		        
			}
			else
				Log.d(WiFiDirectActivity.TAG, "Remote FileURI Not Found");
			
			if (socket != null) {
                if (socket.isConnected()) {
                    socket.close();
                }
            }
		}
		catch(IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
		}
		catch (InvalidKeyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchPaddingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public void onSessionKeyGeneratedFailure(Socket remote, Exception e) {
		Log.e(this.toString(), "entered onSessionKeyGeneratedFailure", e);
		
		mNotificationManager.cancel(NOTIF_AUTH_STARTED);		
		
		//Failure notification
		Notification.Builder mBuilder =
	        new Notification.Builder(getApplicationContext())
	        .setSmallIcon(android.R.drawable.ic_dialog_alert)
	        .setContentTitle("Authentication failed!")
	        .setAutoCancel(true)
	        .setTicker("Authentication failed!")
	        .setContentText("Failure during authentication with ambient audio!");	
		
		// Publish notification.
		mNotificationManager.notify(
					NOTIF_AUTH_FAILURE, mBuilder.getNotification());
		
		if (socket != null) {
            if (socket.isConnected()) {
                try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
					Log.e(WiFiDirectActivity.TAG, e1.getMessage());
				}
            }
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
		mNotificationManager.notify(
					NOTIF_AUTH_STARTED, mBuilder.getNotification());		
	}
}
