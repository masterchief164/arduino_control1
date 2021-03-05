package com.example.arduino_control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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
import android.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class mainbkp extends AppCompatActivity {

    private final static int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;
    public static Handler handler;
    public static BluetoothSocket mobSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectedThread createConnectedThread;
    private String dev_name = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayoutbkp);
        Button bluetooth_connect = findViewById(R.id.bluetooth_connect);
        Toolbar toolbar = findViewById(R.id.toolbar);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        SeekBar seekBar_led = findViewById(R.id.seekBar_led);
        SeekBar seekBar_fan = findViewById(R.id.seekBar_fan);
        TextView textViewFan = findViewById(R.id.textView_fan);
        TextView textViewLed = findViewById(R.id.textView_led);
        SwitchMaterial switch_led = findViewById(R.id.switch_led);
        SwitchMaterial switch_fan = findViewById(R.id.switch_fan);


        dev_name = getIntent().getStringExtra("deviceName");

        if (dev_name != null) {
            toolbar.setSubtitle("Connecting to " + dev_name + ".....");
            progressBar.setVisibility(View.VISIBLE);
            bluetooth_connect.setEnabled(false);

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectedThread = new CreateConnectedThread(bluetoothAdapter, null);
            connectedThread.start();
        }


        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case CONNECTING_STATUS:

                        switch (msg.arg1) {

                            case 1:
                                toolbar.setSubtitle("Connected to " + dev_name);
                                progressBar.setVisibility(View.GONE);
                                bluetooth_connect.setEnabled(true);
                                break;
                            case 2:
                                toolbar.setSubtitle("Device failed to connect");
                                progressBar.setVisibility(View.GONE);
                                bluetooth_connect.setEnabled(true);
                                break;
                        }
                        break;
                    case MESSAGE_READ:
                        String ardmsg = msg.obj.toString();
                        switch (ardmsg.toLowerCase()) {
                            case "led on":
                                switch_led.setChecked(false);
                                break;
                            case "led off":
                                switch_led.setChecked(true);
                                break;
                        }
                }

            }
        };

        bluetooth_connect.setOnClickListener(v -> {
            Intent intent = new Intent(mainbkp.this, SelectDeviceActivity.class);
            startActivity(intent);
        });

        switch_led.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String cmdTxt;
            boolean state = switch_led.isChecked();
            if (state) {
                switch_led.setChecked(false);
                cmdTxt = "<turn on>";
            } else {
                switch_led.setChecked(true);
                cmdTxt = "<turn off>";
            }
            connectedThread.write(cmdTxt);
        });


        seekBar_led.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float k = (float) (100.0 * progress / 256);
                textViewLed.setText((getString(R.string.size_is) + k));
                textViewLed.setTextSize(k);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (createConnectedThread != null) {
            createConnectedThread.cancel();
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public static class CreateConnectedThread extends Thread {
        public CreateConnectedThread(BluetoothAdapter bluetoothAdapter, String dev_address) {

            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(dev_address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(null, "Socket's create method failed", e);
            }
            mobSocket = tmp;
        }

        public void run() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                mobSocket.connect();
                Log.e("Status", "Device Connected");
            } catch (IOException error) {
                try {
                    mobSocket.close();
                    Log.e("Status", "Cannot Connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeExc) {
                    Log.e("Status", "Could not close the socket", closeExc);
                }
                return;
            }
            try {
                connectedThread = new ConnectedThread(mobSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectedThread.run();
        }

        public void cancel() {
            try {
                mobSocket.close();
            } catch (IOException cloexc) {
                Log.e("Status", "Could not close the socket");
            }
        }
    }

    public static class ConnectedThread extends Thread {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) throws IOException {
            InputStream tmpIn;
            OutputStream tmpout;


            tmpIn = socket.getInputStream();
            tmpout = socket.getOutputStream();

            inputStream = tmpIn;
            outputStream = tmpout;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while (true) {
                try {
                    buffer[bytes] = (byte) inputStream.read();
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

        public void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }


    }
}
