package com.example.opalnativetest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
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

        // context-registered Broadcast Receiver - via Context
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addCategory(Intent.CATEGORY_APP_FILES);
        registerReceiver(new TestBroadcastReceiver2(), intentFilter);
        registerReceiver(receiverFromMethod(),intentFilterFromMethod());

        // context-registered Broadcast Receiver - via LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new TestBroadcastReceiver2(), new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private BroadcastReceiver receiverFromMethod() {
        return new TestBroadcastReceiver1();
    }

    private IntentFilter intentFilterFromMethod() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addCategory(Intent.CATEGORY_APP_CALENDAR);
        return intentFilter;
    }

    /**
     * A native method that is implemented by the 'opalnativetest' native library,
     * which is packaged with this application.
     */
    public native String someNativeMethod(int input);
}