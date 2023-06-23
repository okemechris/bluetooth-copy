package com.example.copyclient;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class CustomServiceConnection implements ServiceConnection {

    public static ClipboardService myService;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
         myService = ((ClipboardService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
