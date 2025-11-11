package com.example.thermolink.bluetooth;

import static androidx.core.content.ContextCompat.registerReceiver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyBluetoothHelper {
    private final String TAG = "BluetoothHelper";
    private ConnectionListener connectionListener;
    private static MyBluetoothHelper instance;
    private List<BluetoothDevice> visibleDevices = new ArrayList<>();

    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    private MyBluetoothHelper(Context context) {
        this.context = context.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static MyBluetoothHelper getInstance(Context context){
        if (instance == null){
            instance = new MyBluetoothHelper(context);
        }
        return instance;
    }

    // ----------------------------
    // Перевірки Bluetooth
    // ----------------------------

    public void setConnectionListener(ConnectionListener connectionListener){
        this.connectionListener = connectionListener;
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable(); // на Android 12+ може не спрацювати без дозволу
        }
    }

    // ----------------------------
    // Перевірка дозволів
    // ----------------------------
    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // до Android 12 дозволи не потрібні
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // ----------------------------
    // Публічні методи
    // ----------------------------

    // Повертає список спарених пристроїв
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devicesList = new ArrayList<>();
        if (!hasBluetoothConnectPermission()) {
            // Користувач не дав дозвіл
            return devicesList;
        }

        if (bluetoothAdapter != null) {
            try {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    devicesList.add(device);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        return devicesList;
    }

    // Запуск сканування нових пристроїв
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean startDiscovery() {
        // Перевіряємо дозвіл
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Регіструємо ресівер
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothReceiver, filter);

        // Запускаємо сканування
        if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
            return bluetoothAdapter.startDiscovery();
        }
        return false;
    }


    // Зупинка сканування
    public void cancelDiscovery() {
        if (!hasBluetoothScanPermission()) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }


    //
    public List<BluetoothDevice> getVisibleDevices(){
        return visibleDevices;
    }

    //Бродкаст

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !visibleDevices.contains(device)) {
                    visibleDevices.add(device);
                    Log.d(TAG, "Broadcast: found " + device.getName());
                    if (connectionListener != null) {
                        connectionListener.onChanged(device, true);
                    }

                }
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device!= null){
                    visibleDevices.remove(device);

                    if (connectionListener != null) {
                        connectionListener.onChanged(device, false);
                    }
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d(TAG, "Broadcast: scanning finished \nUnregistering broadcast...");
                context.unregisterReceiver(bluetoothReceiver);

            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d(TAG, "Broadcast: scanning started");
            }
        }
    };


    public interface ConnectionListener {
        void onChanged(BluetoothDevice device, boolean isConnected);
    }

}
