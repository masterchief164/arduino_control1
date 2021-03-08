package com.example.arduino_control;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private String deviceName = null;
    public BluetoothAdapter bluetoothAdapter = null;

    static final int req = 123;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.bluetoothConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        SwitchMaterial led_switch = findViewById(R.id.switch_led);
        SwitchMaterial fan_switch = findViewById(R.id.switch_fan);
        SeekBar led_seekbar = findViewById(R.id.seekBar_led);
        SeekBar fan_seekbar = findViewById(R.id.seekBar_fan);
        TextView fan_textview = findViewById(R.id.textView_fan);
        TextView led_textview = findViewById(R.id.textView_led);
        fan_seekbar.setEnabled(false);
        fan_switch.setEnabled(false);
        led_switch.setEnabled(false);
        led_seekbar.setEnabled(false);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_ADMIN) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.BLUETOOTH) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Grant Permissions");
                builder.setMessage("Grant Bluetooth and Location Access");
                builder.setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH
                        }, req
                ));
                builder.setNegativeButton("Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            } else {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH
                        }, req
                );
            }
        }

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            String deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            checkBTState();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                fan_switch.setEnabled(true);
                                led_switch.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.toLowerCase()) {
                            case "led on":
                                led_textview.setText(arduinoMsg.toUpperCase());
                                led_switch.setChecked(true);
                                led_seekbar.setProgress(1);
                                break;
                            case "led off":
                                led_textview.setText(arduinoMsg.toUpperCase());
                                led_switch.setChecked(false);
                                led_seekbar.setProgress(0);
                                break;
                            case "fan on":
                                fan_switch.setChecked(true);
                                fan_textview.setText(arduinoMsg.toUpperCase());
                                fan_seekbar.setProgress(1);
                                break;
                            case "fan off":
                                fan_switch.setChecked(false);
                                fan_textview.setText(arduinoMsg.toUpperCase());
                                fan_seekbar.setProgress(0);
                                break;
                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(view -> {
            // Move to adapter list
            Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
            startActivity(intent);
        });


        led_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String cmdTxt;
            boolean state = led_switch.isChecked();
            if (state) {
                led_switch.setChecked(true);
                led_textview.setText(R.string.swion);
                led_seekbar.setProgress(1);
                led_seekbar.setEnabled(true);
                cmdTxt = "led on";
            } else {
                led_switch.setChecked(false);
                led_textview.setText(R.string.swioff);
                led_seekbar.setProgress(0);
                led_seekbar.setEnabled(false);

                cmdTxt = "led off";
            }
            connectedThread.write(cmdTxt);
        });
        fan_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String cmdTxt;
            boolean state = fan_switch.isChecked();
            if (state) {
                fan_switch.setChecked(true);
                fan_textview.setText(R.string.swion);
                fan_seekbar.setProgress(1);
                fan_seekbar.setEnabled(true);
                cmdTxt = "fan on";

            } else {
                fan_switch.setChecked(false);
                fan_textview.setText(R.string.swioff);
                fan_seekbar.setProgress(0);
                fan_seekbar.setEnabled(false);
                cmdTxt = "fan off";
            }
            connectedThread.write(cmdTxt);
        });
        led_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String cmdTxt;
                if (seekBar.getProgress() == 0) {
                    led_switch.setChecked(false);
                    led_textview.setText(R.string.swioff);
                    cmdTxt = "led off";
                } else {
                    led_switch.setChecked(true);
                    led_textview.setText(R.string.swion);
                    cmdTxt = "led intensity " + seekBar.getProgress();
                    connectedThread.write(cmdTxt);
                }
            }
        });
        fan_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String cmdTxt;
                if (seekBar.getProgress() == 0) {
                    fan_switch.setChecked(false);
                    fan_textview.setText(R.string.swioff);
                    cmdTxt = "fan off";
                } else {
                    fan_switch.setChecked(true);
                    fan_textview.setText(R.string.swion);
                    cmdTxt = "fan intensity " + seekBar.getProgress();
                    connectedThread.write(cmdTxt);
                }
            }
        });
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null) {
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == req) {
            if ((grantResults.length > 0) &&
                    (grantResults[0] +
                            grantResults[1] +
                            grantResults[2] +
                            grantResults[3] +
                            grantResults[4]
                            == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(),
                        "permissions are granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
        } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
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
                Log.e(TAG, "Socket's create() method failed", e);
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

                Log.d("Status", "Trying to connect");
                mmSocket.connect();
                Log.d("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.d("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            try {
                connectedThread = new ConnectedThread(mmSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) throws IOException {
            InputStream tmpIn;
            OutputStream tmpOut;

            // Get the input and output streams, using temp objects because
            // member streams are final
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();

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
                    if (buffer[bytes] == '\n') {
                        readMessage = new String(buffer, 0, bytes);
                        Log.e("Arduino Message", readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
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
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */

    }
}