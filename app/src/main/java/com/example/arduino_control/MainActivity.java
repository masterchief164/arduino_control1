package com.example.arduino_control;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SeekBar seekBar_led = findViewById(R.id.seekBar_led);
        SeekBar seekBar_fan = findViewById(R.id.seekBar_fan);
        TextView textViewFan = findViewById(R.id.textView_fan);
        TextView textViewLed = findViewById(R.id.textView_led);

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
}