package com.example.scancommand;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // The HC-05 address
    private static final String MAC = "98:D3:11:FC:69:25";
    private static boolean unlocked = false;

    public static BluetoothSocket mmSocket;
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ID = 1;

    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

    // This will be used to scan if location services are enabled
    Handler locationHandler = new Handler();
    Runnable runnable;
    int delay = 5000;

    public static Handler connectionHandler;
    public static boolean deviceFound;
    public static boolean connected;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private final static int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView t = findViewById(R.id.info);

        button  = findViewById(R.id.unlockButton);
        button.setClickable(false);
        button.setEnabled(false);

        if (bluetoothAdapter == null) {
            t.setText("Device doesn't support Bluetooth\n");
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //locationEnabled();

        requestPermissions();

//        if (pairedDevices.size() > 0) {
//            // There are paired devices. Get the name and address of each paired device.
//            for (BluetoothDevice device : pairedDevices) {
//                String deviceName = device.getName();
//                String deviceHardwareAddress = device.getAddress(); // MAC address
//            }
//        }

        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        boolean discover = bluetoothAdapter.startDiscovery();

        Log.i(TAG, "DISCOVER");


        connectionHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
//                    case CONNECTING_STATUS:
//                        switch (msg.arg1) {
//                            case 1:
//                                toolbar.setSubtitle("Connected to " + deviceName);
//                                progressBar.setVisibility(View.GONE);
//                                buttonConnect.setEnabled(true);
//                                buttonToggle.setEnabled(true);
//                                break;
//                            case -1:
//                                toolbar.setSubtitle("Device fails to connect");
//                                progressBar.setVisibility(View.GONE);
//                                buttonConnect.setEnabled(true);
//                                break;
//                        }
//                        break;
                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        TextView message = findViewById(R.id.message);
                        switch (arduinoMsg.toLowerCase()) {
                            case "0":
                                message.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "1":
                                message.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;
                }
            }
        };


        }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            short rssi = 0;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                TextView t = findViewById(R.id.info);

                StringBuilder sb = new StringBuilder(7);
                sb.append("Name: ").append(device.getName()).append("\nAddress: ").
                        append(device.getAddress()).append("\nRSSI:" ).append(rssi).append("dbm");

                t.setText(sb.toString());

                if(device.getAddress().equals(MAC)) {
                    deviceFound = true;
                }

//                Log.i(TAG, sb.toString());
            }
        }
    };




    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
        bluetoothAdapter.cancelDiscovery();
    }

    private void requestPermissions(){
        int androidVersion = Build.VERSION.SDK_INT;
        if (androidVersion >= 23){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    }, REQUEST_ID);
        }
    }

    private boolean locationEnabled () {
        LocationManager lm = (LocationManager)
                getSystemService(Context. LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MainActivity. this )
                    .setMessage( "GPS Enable" )
                    .setPositiveButton( "Settings" , new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                }
                            })
                    .setNegativeButton( "Cancel" , null )
                    .show() ;
            return false;
        }
        return true;
    }

    public void onClickBtn(View v)
    {
        if(connected && !unlocked) {
            connectedThread.write("UNLOCK!");
            unlocked = true;
            button.setText("UNLOCKED");
            TextView t = findViewById(R.id.message);
//            t.setText(new StringBuilder(2).append("Message: ")
//                    .append(connectedThread.message));
            return;
        }
        if(connected && unlocked) {
            connectedThread.write("LOCK!");
            unlocked = false;
            button.setText("LOCKED");
            TextView t = findViewById(R.id.message);
//            t.setText(new StringBuilder(2).append("Message: ")
//                    .append(connectedThread.message));
            return;
        }
    }

    @Override
    protected void onResume() {
        // Check is the location is enabled in order to discover, connect and send data
        locationHandler.postDelayed(runnable = new Runnable() {
            public void run() {
                locationHandler.postDelayed(runnable, delay);
                if(locationEnabled()) {

                    bluetoothAdapter.startDiscovery();

                    if(deviceFound && !connected){
                        createConnectThread = new CreateConnectThread(bluetoothAdapter, MAC);
                        createConnectThread.start();
                    }

                    if(connected){
                        button.setClickable(true);
                        button.setEnabled(true);
                    }
                    else{
                        button.setClickable(false);
                        button.setEnabled(false);
                    }
                }
            }
        }, delay);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationHandler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e("CONNECT", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.i("Status", "Device connected");
                //connectionHandler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
                connected = true;
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.i("Status", "Cannot connect to device");
                    //connectionHandler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                    connected = false;
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                connected = false;
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        connectionHandler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}


