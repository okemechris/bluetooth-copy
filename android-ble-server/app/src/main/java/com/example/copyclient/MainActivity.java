package com.example.copyclient;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.copyclient.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> enableBtLauncher;
    private ActivityResultLauncher<String> requestBluetoothPermissionLauncher;
    private static final int REQUEST_ENABLE_BT_ADVERTISE = 1;
    public static final String STOP_FOREGROUND_ACTION = "STOP_ACTION";
    public static final String START_FOREGROUND_ACTION = "START_ACTION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);


        requestBluetoothPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission is granted, you can proceed with Bluetooth operations
                        Snackbar.make(binding.getRoot(), "Permission granted", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else {
                        // Permission is not granted, handle the error or show a message to the user
                        Snackbar.make(binding.getRoot(), "Permission not granted", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                });


        Button startButton = findViewById(R.id.startButton);
        startButton.setEnabled(false);
        startButton.setBackgroundColor(getColor(android.R.color.holo_blue_dark));

        String bluetoothPermission = Manifest.permission.BLUETOOTH;

        if (ContextCompat.checkSelfPermission(this, bluetoothPermission) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissionLauncher.launch(bluetoothPermission);
        }else {
            startButton.setEnabled(true);
        }


        requestBluetoothAdvertisePermission();
        requestBluetoothConnectPermission();

        enableBtLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Bluetooth is enabled, you can proceed with your logic here
                        startButton.setEnabled(true);
                    } else {
                        // Bluetooth is not enabled, handle the error or show a message to the user
                        Snackbar.make(startButton.getRootView(), "Enable bluetooth", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
        });


        AtomicBoolean isStarted = new AtomicBoolean(false);


       startButton.setOnClickListener(v -> {

           if (isStarted.get()){
               Intent stopServiceIntent = new Intent(this, ClipboardService.class);
               stopServiceIntent.setAction(STOP_FOREGROUND_ACTION);
               startService(stopServiceIntent);
               isStarted.set(false);
               startButton.setBackgroundColor(getColor(android.R.color.holo_blue_dark));
               startButton.setText("START");
           }else{
               BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

               if (bluetoothAdapter != null) {
                   if (!bluetoothAdapter.isEnabled()) {
                       Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                       if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                           enableBtLauncher.launch(enableBtIntent);
                       }
                   }else{
                       Intent startServiceIntent = new Intent(this, ClipboardService.class);
                       startServiceIntent.setAction(START_FOREGROUND_ACTION);
                       startService(startServiceIntent);
                       // Bind to the service.
                       CustomServiceConnection conn = new CustomServiceConnection();
                       bindService(startServiceIntent, conn, Context.BIND_AUTO_CREATE);
                       isStarted.set(true);
                       startButton.setText("STOP");
                       startButton.setBackgroundColor(getColor(android.R.color.holo_red_light));
                   }
               }else{
                   Snackbar.make(v, "Enable bluetooth", Snackbar.LENGTH_LONG)
                           .setAction("Action", null).show();
               }
           }


       });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    private void requestBluetoothAdvertisePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},
                        REQUEST_ENABLE_BT_ADVERTISE);
            }
        }
    }

    private void requestBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_ENABLE_BT_ADVERTISE);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ENABLE_BT_ADVERTISE) {
            if (grantResults.length > 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, show a message or take appropriate action
                Toast.makeText(this, "Bluetooth advertising permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}