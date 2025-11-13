package com.example.thermolink.bluetooth;

import static androidx.core.content.ContextCompat.registerReceiver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MyBluetoothHelper {
    /// Bluetooth Server
    private BluetoothServerSocket server;
    private BluetoothDevice targetDevice;
    private UUID serviceUUID = null;
    private BluetoothGatt gatt;
    /// Bluetooth server
    private final String TAG = "BluetoothHelper";
    private ConnectionListener connectionListener;
    private static MyBluetoothHelper instance;
    private List<BluetoothDevice> visibleDevices = new ArrayList<>();

    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    //BLE
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;
    private MyBluetoothHelper(Context context) {
        this.context = context.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_UUID);
        context.registerReceiver(uuid_receiver, filter);

    }
/*
        // Регіструємо ресівер
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothReceiver, filter);
 */
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
    //server
    @SuppressLint("MissingPermission")
    public void connect (BluetoothDevice device){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            gatt = device.connectGatt(context, false, gattCallback);
        }
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Підключено до AT-09!");
                gatt.discoverServices(); // Шукаємо сервіси
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("BLE", "Відключено");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Сервіси знайдено!");
                findFFE0Service(gatt);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void findFFE0Service(BluetoothGatt gatt) {
        UUID serviceUUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
        UUID charUUID   = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");

        BluetoothGattService service = gatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);
            if (characteristic != null) {
                // Вмикаємо сповіщення (якщо підтримує)
                gatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                );
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }

                // Тепер можеш читати/писати:
                // gatt.readCharacteristic(characteristic);
                // writeToCharacteristic(gatt, characteristic, "Hello AT-09!");
            }
        }
    }

    //send data
//    public void sendData(String message){
//        try{
//            BluetoothSocket socket = ;
//            OutputStream outputStream = socket.getOutputStream();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

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
            visibleDevices.clear();
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
                if (device != null && !visibleDevices.contains(device)&& device.getName() != null) {
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
                Log.d(TAG, "Broadcast: scanning finished\n"+ MyBluetoothHelper.getInstance(context).getVisibleDevices() + "\nUnregistering broadcast...");
                context.unregisterReceiver(bluetoothReceiver);
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d(TAG, "Broadcast: scanning started");
            }


            }
        };

    private final BroadcastReceiver uuid_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                Log.d("DEBUG", "Code here: " + Arrays.toString(uuids));

                if (device.equals(targetDevice) && uuids != null && uuids.length > 0) {
                    // Беремо перший UUID (або шукай потрібний)
                    if (uuids != null && uuids.length > 0) {
                        for (Parcelable p : uuids) {
                            if (p instanceof ParcelUuid) {
                                ParcelUuid parcelUuid = (ParcelUuid) p;
                                serviceUUID = parcelUuid.getUuid(); // Ось тут — java.util.UUID
                                Log.d("Bluetooth", "UUID: " + serviceUUID);
                                break;
                            }
                        }
                    }
                }

                Log.d("Bluetooth", "Знайдено UUID: " + serviceUUID);

                // Тепер підключаємося
                //connectWithUUID(targetDevice, serviceUUID);

                // Відписуємося, якщо більше не потрібно

            }

        }
    };
    public interface ConnectionListener {
        void onChanged(BluetoothDevice device, boolean isConnected);
    }

    };



