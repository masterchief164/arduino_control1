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
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.agilie.circularpicker.ui.view.CircularPickerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private final static int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;
    public static int CONNECTED_STATUS = 0;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private String deviceName = null;
    public BluetoothAdapter bluetoothAdapter = null;
    SwitchMaterial led_switch;
    SwitchMaterial fan_switch;
    CircularPickerView led_seekbar;
    CircularPickerView fan_seekbar;
    TextView fan_textview;
    TextView led_textview;
    FloatingActionButton floatingActionButton;
    TextView voiceResults;


    static final int req = 123;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //                              UI Initialization
        final Button buttonConnect = findViewById(R.id.bluetoothConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        led_switch = findViewById(R.id.switch_led);
        fan_switch = findViewById(R.id.switch_fan);
        led_seekbar = findViewById(R.id.seekBar_led);
        fan_seekbar = findViewById(R.id.seekBar_fan);
        fan_textview = findViewById(R.id.textView_fan);
        led_textview = findViewById(R.id.textView_led);
        floatingActionButton = findViewById(R.id.voice_activation);
        voiceResults = findViewById(R.id.voiceResults);
        progressBar.setVisibility(View.GONE);
        led_seekbar.setMaxValue(100);
        led_seekbar.setMaxLapCount(1);
        led_seekbar.setCenteredText(0 + "%");
        fan_seekbar.setMaxValue(100);
        fan_seekbar.setMaxLapCount(1);
        fan_seekbar.setCenteredText(0 + "%");


        //                          Checking Permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_ADMIN) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.INTERNET) +
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.BLUETOOTH_ADMIN) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.BLUETOOTH) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this
                            , Manifest.permission.INTERNET)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Grant Permissions");
                builder.setMessage("Grant Bluetooth and Location Access");
                builder.setPositiveButton("OK", (dialog, which) ->
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                        Manifest.permission.BLUETOOTH_ADMIN,
                                        Manifest.permission.BLUETOOTH,
                                        Manifest.permission.RECORD_AUDIO,
                                        Manifest.permission.INTERNET
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
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.INTERNET
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

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }


        //                          voice button


        floatingActionButton.setOnClickListener(v -> {
            Intent speechRecognizerIntent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault());
            startActivityForResult(speechRecognizerIntent, 10);
        });

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
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.charAt(0)) {
                            case '1':
                                led_textview.setText(R.string.swion);
                                led_switch.setChecked(true);
                                led_seekbar.setCurrentValue(50);
                                break;
                            case '2':
                                led_textview.setText(R.string.swioff);
                                led_switch.setChecked(false);
                                led_seekbar.setCurrentValue(0);
                                break;
                            case '3':
                                fan_switch.setChecked(true);
                                fan_textview.setText(R.string.swion);
                                fan_seekbar.setCurrentValue(50);
                                break;
                            case '4':
                                fan_switch.setChecked(false);
                                fan_textview.setText(R.string.swioff);
                                fan_seekbar.setCurrentValue(0);
                                break;
                            case '5':
                                int k = Integer.parseInt(arduinoMsg.substring(2));
                                led_switch.setChecked(true);
                                led_seekbar.setCurrentValue(k);
                                Log.e(TAG, String.valueOf(k));
                                break;
                            case '6':
                                k = Integer.parseInt(arduinoMsg.substring(2));
                                fan_switch.setChecked(true);
                                fan_seekbar.setCurrentValue(k);
                                Log.e(TAG, String.valueOf(k));
                                break;
                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(view -> {
            // Move to adapter list
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            checkBTState();
        });


        //                              Led Switch
        led_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String cmdTxt;
            boolean state = led_switch.isChecked();
            if (state) {
                led_switch.setChecked(true);
                led_textview.setText(R.string.swion);
                led_seekbar.setCurrentValue(50);
                cmdTxt = "1";
            } else {
                led_switch.setChecked(false);
                led_textview.setText(R.string.swioff);
                led_seekbar.setCurrentValue(0);
                cmdTxt = "2";
            }
            if (CONNECTED_STATUS == 1)
                connectedThread.write(cmdTxt);
        });

        //                              Fan Switch
        fan_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String cmdTxt;
            boolean state = fan_switch.isChecked();
            if (state) {
                fan_switch.setChecked(true);
                fan_textview.setText(R.string.swion);
                fan_seekbar.setCurrentValue(50);
                cmdTxt = "3";

            } else {
                fan_switch.setChecked(false);
                fan_textview.setText(R.string.swioff);
                fan_seekbar.setCurrentValue(0);
                cmdTxt = "4";
            }
            if (CONNECTED_STATUS == 1)
                connectedThread.write(cmdTxt);
        });

        //                              Led SeekBar
        led_seekbar.setValueChangedListener(i -> {
            String cmdTxt;
            if (i == 0) {
                led_switch.setChecked(false);
                led_textview.setText(R.string.swioff);
                led_seekbar.setCenteredText(0 + "%");
            } else {
                led_switch.setChecked(true);
                led_textview.setText(R.string.swion);
                cmdTxt = "5 " + i;
                led_seekbar.setCenteredText(i + "%");
                if (CONNECTED_STATUS == 1) {
                    connectedThread.write(cmdTxt);
                    Log.e("Val out", cmdTxt);
                }
            }
        });


        //                              Fan SeekBar

        fan_seekbar.setValueChangedListener(i -> {
            String cmdTxt;
            if (i == 0) {
                fan_switch.setChecked(false);
                fan_textview.setText(R.string.swioff);
                fan_seekbar.setCenteredText(0 + "%");
            } else {
                fan_switch.setChecked(true);
                fan_textview.setText(R.string.swion);
                fan_seekbar.setCenteredText(i + "%");
                cmdTxt = "6 " + i;
                if (CONNECTED_STATUS == 1)
                    connectedThread.write(cmdTxt);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String rec = null;
            for (String val : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)) {
                if (val.toLowerCase().contains("led")) {
                    boolean state = led_switch.isChecked();
                    if (state && val.toLowerCase().contains("off")) {
                        rec = val.toUpperCase();
                        led_switch.setChecked(!state);
                    } else if (!state && val.toLowerCase().contains("on")) {
                        rec = val.toUpperCase();
                        led_switch.setChecked(!state);
                    } else {
                        rec = val.toUpperCase();
                    }
                    break;
                } else if (val.toLowerCase().contains("fan")) {
                    boolean state = fan_switch.isChecked();
                    if (state && val.toLowerCase().contains("off")) {
                        rec = val.toUpperCase();
                        fan_switch.setChecked(!state);
                    } else if (!state && val.toLowerCase().contains("on")) {
                        rec = val.toUpperCase();
                        fan_switch.setChecked(!state);
                    } else {
                        rec = val.toUpperCase();
                    }

                } else if (val.toLowerCase().contains("connect")) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    checkBTState();
                }
            }
            if (rec != null) {
                voiceResults.setText(rec);
            }
        }
    }

    /* ======================= Terminate Connection at BackPress ====================== */
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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == req) {
            if ((grantResults.length > 0) &&
                    (grantResults[0] +
                            grantResults[1] +
                            grantResults[2] +
                            grantResults[3] +
                            grantResults[4] +
                            grantResults[5] +
                            grantResults[6]
                            == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(),
                        "permissions are granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        //          Emulator doesn't support Bluetooth and will return null
        if (bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
            Intent intent = new Intent(MainActivity.this,
                    SelectDeviceActivity.class);
            startActivity(intent);
        } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    /* ================== Thread to Create Bluetooth Connection ========================== */
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
                Due to Android device varieties,the method below
                 may not work fo different devices.
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
                CONNECTED_STATUS = 1;
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

    /* =============================== Thread for Data Transfer ===================== */
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
                    Read from the InputStream from Arduino
                    until termination character is reached.
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