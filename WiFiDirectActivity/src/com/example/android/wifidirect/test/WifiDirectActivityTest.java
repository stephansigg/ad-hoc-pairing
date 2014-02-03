package com.example.android.wifidirect.test;

import java.io.IOException;
import java.net.Socket;

import com.example.android.wifidirect.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class WifiDirectActivityTest extends Activity {
	
	private AmbientAudioClientTest client = null;
	private AmbientAudioServerTest server = null;
	private Socket socket = null;
	
	private Button btnStartClient = null;
	private Button btnStartServer = null;
	
	private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecord/";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_direct_activity_test);
		
		btnStartClient = (Button)findViewById(R.id.startClient);
		btnStartClient.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				client = new AmbientAudioClientTest(getApplicationContext());
				
				client.startRecording();
				
				client.calculateSharedKey(1);
				
				String fileIn = path  + "Android.png";
				String fileOut = path + "Android_ENCRYPTED.png";
				client.encrypt(fileIn, fileOut);
			}
		});
		
		btnStartServer = (Button)findViewById(R.id.startServer);
		btnStartServer.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				server = new AmbientAudioServerTest(getApplicationContext());
				
				server.startRecordingAndCalculation(1);
				
				notifyAuthenticationStart();
				String fileIn = path  + "Android.png";
				String fileOut = path + "Android_ENCRYPTED.png";
				server.encrypt(fileIn, fileOut);
				
				fileIn = fileOut;
				fileOut = path + "Android_DECRYPTED.png";
				server.decrypt(fileIn, fileOut);
				
				showImage(fileOut);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wifi_direct_activity_test, menu);
		return true;
	}
	
	private void showImage(String filePath) {
		Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + filePath), "image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        (getApplicationContext()).startActivity(intent);
	}
	
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
					1, mBuilder.getNotification());		
	}

}
