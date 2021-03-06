/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.authentication.ambientaudio.AmbientAudioClient;
import org.authentication.ambientaudio.AmbientAudioPairing;
import org.authentication.ambientaudio.AmbientAudioServer;
import org.authentication.ambientaudio.CipherUtils;
import org.authentication.ambientaudio.OnAmbientAudioResultListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        
        //prepare file transfer service (of client)
        SimpleLog.appendLog("Preparing to send " + uri.toString() + 
        		" to " + info.groupOwnerAddress.toString());
        
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
        
        SimpleLog.appendLog("Successfully start file transfer service");
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
        	
        	SimpleLog.appendLog("Group Owner - Server: " + info.groupOwnerAddress.toString());
        	
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();
        } else if (info.groupFormed) {

        	SimpleLog.appendLog("Client connected to: " + info.groupOwnerAddress.toString());
        	
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }
// each device is a file server
// and can send files to other devices
//        new FileServerAsyncTask(getActivity(), 
//        		mContentView.findViewById(R.id.status_text))
//        	.execute();
//        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
//        ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
//              .getString(R.string.client_text));

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> implements OnAmbientAudioResultListener{

    	private static int NOTIF_AUTH_STARTED = 1;
    	private static int NOTIF_AUTH_SUCCESS = 2;
    	private static int NOTIF_AUTH_FAILURE = 3;
    	
        private Context context;
        private TextView statusText;
        private NotificationManager mNotificationManager;
        
        private AmbientAudioServer ambientAudioServer = null;
        private ServerSocket serverSocket = null;
        private Socket client = null;
        private String fileName = null;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                
                SimpleLog.appendLog("Server started listening at " + serverSocket.toString());
                
                //may be blocked here, waiting...
                client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                
                if(ambientAudioServer == null)
                	ambientAudioServer = new AmbientAudioServer(client, context, this);
                //notifyAuthenticationStart();
                
                SimpleLog.appendLog("Authentication Server started at " + serverSocket.toString() + " with the client " + client.toString());
                
                // successful authentication...
                //return f.getAbsolutePath();
                //return client.getInetAddress().toString();
                return client.toString();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                
                SimpleLog.appendLog("Authentication Server failed due to " + e.getMessage());
                
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

		@Override
		public void onSessionKeyGeneratedSuccess(byte[] key, Socket remote) {
			Log.i(this.toString(),"entered onSessionKeyGeneratedSuccess");
			
			SimpleLog.appendLog("Authentication Server succeeded with " + remote.toString());
			
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
				final File f = new File(Environment.getExternalStorageDirectory() + "/"
	                    + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
	                    + ".jpg");
	
	            File dirs = new File(f.getParent());
	            if (!dirs.exists())
	                dirs.mkdirs();
	            f.createNewFile();
	
	            Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
	            InputStream inputstream = remote.getInputStream();
	            copyFile(inputstream, new FileOutputStream(f));
	            serverSocket.close();
	            
	            fileName = f.getParent() + "/DECRYPTED_" + f.getName();
				CipherUtils.decrypt(f.getAbsolutePath(), fileName, key);
				
				SimpleLog.appendLog("Athentication Server saved the decrypted file: " + fileName);
				
				if (fileName != null) {
	                statusText.setText("File copied - " + fileName);
	                Intent intent = new Intent();
	                intent.setAction(android.content.Intent.ACTION_VIEW);
	                intent.setDataAndType(Uri.parse("file://" + fileName), "image/*");
	                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                context.startActivity(intent);
	            }
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onSessionKeyGeneratedFailure(Socket remote, Exception e) {
			Log.e(this.toString(), "entered onSessionKeyGeneratedFailure", e);
			
			SimpleLog.appendLog("Authentication Server failed with " + remote.toString() + " : " + e.getMessage());
			mNotificationManager.cancel(NOTIF_AUTH_STARTED);		
			
			//Failure notification
			Notification.Builder mBuilder =
		        new Notification.Builder(context)
		        .setSmallIcon(android.R.drawable.ic_dialog_alert)
		        .setContentTitle("Authentication failed!")
		        .setAutoCancel(true)
		        .setTicker("Authentication failed!")
		        .setContentText("Failure during authentication with ambient audio!");	
			
			// Publish notification.
			mNotificationManager.notify(
						NOTIF_AUTH_FAILURE, mBuilder.getNotification());
		}
		
		/**
		 * Notify user that authentication started
		 */
		public void notifyAuthenticationStart() {
			Notification.Builder mBuilder =
		        new Notification.Builder(context)
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

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
