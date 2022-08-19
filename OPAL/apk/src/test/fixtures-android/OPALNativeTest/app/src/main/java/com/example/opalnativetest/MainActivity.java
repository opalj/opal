package com.example.opalnativetest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import com.example.opalnativetest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'opalnativetest' library on application startup.
    static {
        System.loadLibrary("opalnativetest");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // native method invocation
        int someData = 42;
        TextView tv = binding.sampleText;
        tv.setText(someNativeMethod(someData));

        // context-registered Broadcast Receiver
        registerReceiver(new TestBroadcastReceiver2(), new IntentFilter(Intent.ACTION_POWER_CONNECTED));
    }

    /**
     * A native method that is implemented by the 'opalnativetest' native library,
     * which is packaged with this application.
     */
    public native String someNativeMethod(int input);
}