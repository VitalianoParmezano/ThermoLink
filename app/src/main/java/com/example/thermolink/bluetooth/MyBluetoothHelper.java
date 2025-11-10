package com.example.thermolink.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyBluetoothHelper {

    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    public MyBluetoothHelper(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // ----------------------------
    // Перевірки Bluetooth
    // ----------------------------

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
    public List<String> getPairedDevices() {
        List<String> devicesList = new ArrayList<>();
        if (!hasBluetoothConnectPermission()) {
            // Користувач не дав дозвіл
            return devicesList;
        }

        if (bluetoothAdapter != null) {
            try {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    devicesList.add(device.getName() + "\n" + device.getAddress());
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
        if (!hasBluetoothScanPermission()) {
            // Користувач не дав дозвіл
            return false;
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
            try {
                return bluetoothAdapter.startDiscovery();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // Зупинка сканування
    public void cancelDiscovery() {
        if (!hasBluetoothScanPermission()) {
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
}
