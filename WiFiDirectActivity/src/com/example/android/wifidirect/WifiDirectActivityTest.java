package com.example.android.wifidirect;

import java.io.IOException;
import java.net.Socket;

import org.authentication.ambientaudio.AmbientAudioClient;
import org.authentication.ambientaudio.AmbientAudioPairing;
import org.authentication.ambientaudio.AmbientAudioServer;

import android.os.Bundle;
import android.app.Activity;
import android.app.NotificationManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class WifiDirectActivityTest extends Activity {
	private AmbientAudioPairing ambientAudioPairing = null;
	
	private AmbientAudioClient client = null;
	private AmbientAudioServer server = null;
	private Socket socket = null;
	
	private Button btnStartAuthentication = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_direct_activity_test);
		
		btnStartAuthentication = (Button)findViewById(R.id.startAuthentication);
		btnStartAuthentication.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				client = AmbientAudioPairing.createAmbientAudioClient(socket);
			}
		});
		
		try {
			ambientAudioPairing = AmbientAudioPairing.getInstance(getApplicationContext(), (NotificationManager)getSystemService(NOTIFICATION_SERVICE));
			socket = new Socket();
			socket.bind(null);
			//client = AmbientAudioPairing.createAmbientAudioClient(socket);
			server = AmbientAudioPairing.createAmbientAudioServer(socket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wifi_direct_activity_test, menu);
		return true;
	}

}
