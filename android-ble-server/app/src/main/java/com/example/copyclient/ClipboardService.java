package com.example.copyclient;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.UUID;

public class ClipboardService extends Service {

    private static String MY_UUID = "6a33f2de-f227-11ed-a05b-0242ac120003";
    private static String SERVICE_UUID = "a32edd59-0f12-659f-7114-6bc6853f7b76";

    private static String CHARACTERISTIC_UUID = "6a33f2de-f227-11ed-a05b-0242ac120004";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "clipboard_channel";

    private BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    BluetoothGattServer gattServer;
    BluetoothLeAdvertiser advertiser;
    BluetoothGattCharacteristic characteristic;

    private Handler handler;


    IBinder mBinder = new LocalBinder();


    public class LocalBinder extends Binder {
        public ClipboardService getService() {
            return ClipboardService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Create a handler to send messages to the service.
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(MainActivity.STOP_FOREGROUND_ACTION)) {
            shutdown();
            Log.i(this.getClass().getName(), "Received Stop Foreground Intent ");
            stopForeground(true);
            stopSelfResult(startId);
        }else{
            startService();
        }

        return START_STICKY;
    }

    private void startService() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, CopyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Clipboard Service")
                .setContentText("Listening to clipboard changes")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .addAction(R.drawable.ic_launcher_foreground, "Paste", pendingIntent);

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = notificationBuilder.build();
        } else {
            notification = notificationBuilder.getNotification();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }


        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        characteristic = new BluetoothGattCharacteristic(
                UUID.fromString(CHARACTERISTIC_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(characteristic);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer = bluetoothManager.openGattServer(getApplicationContext(), gattServerCallback);
            gattServer.addService(service);
        }

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                .setIncludeDeviceName(false)
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    @Override
    public void onDestroy() {
        shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Clipboard Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            // Handle connection state changes
            Log.i(this.getClass().getName(), "connection status = " + status);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.i(this.getClass().getName(), "New service added " + service.getUuid());
        }

        @Override
        public void onCharacteristicReadRequest(
                BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            // Handle characteristic read requests
            Log.i(this.getClass().getName(), "Characteristic read request =" + requestId);
        }

        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            // Handle characteristic write requests
            Log.i(this.getClass().getName(), "Characteristic write request =" + requestId);
        }

        // Implement other necessary callback methods
    };

    AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            // Handle characteristic write requests
            Log.i(this.getClass().getName(), "is connectable = " + settingsInEffect.isConnectable());
        }

        @Override
        public void onStartFailure(int errorCode) {
            // Handle characteristic write requests
            Log.i(this.getClass().getName(), "Start failure code = " + errorCode);
        }

    };

    public void sendMessage(String message) {
        // Send the message to the service.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        handler.post(() -> {
            for (BluetoothDevice d : bluetoothManager.getConnectedDevices(BluetoothGatt.GATT)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    int sent = gattServer.notifyCharacteristicChanged(d, characteristic, false, message.getBytes());
                    Log.i(this.getClass().getName(), "===sent-notification==== "+ sent);
                }
            }
        });
    }

    private void shutdown(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattServer.close();
        }

        advertiser.stopAdvertising(advertiseCallback);
    }
}