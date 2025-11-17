package com.example.thermolink.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MyBluetoothHelper {
    private final String TAG = "BluetoothHelper";
    private static MyBluetoothHelper instance;

    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private SearchListener searchListener;
    private ConnectionListener connectionListener;
    private List<BluetoothDevice> visibleDevices = new ArrayList<>();

    // BLE змінні
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic dataCharacteristic;
    private boolean isConnected = false;
    private boolean isReadyToSend = false;
    // Мейн лупер
    private final android.os.Handler mainHandler = new android.os.Handler(
            android.os.Looper.getMainLooper()
    );

    // UUID для HM модулів
    private static final UUID HM_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID HM_CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private MyBluetoothHelper(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static MyBluetoothHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MyBluetoothHelper(context);
        }
        return instance;
    }

    // ----------------------------
    // Публічні методи
    // ----------------------------
    public void setConnectionListener (ConnectionListener connectionListener){
        this.connectionListener = connectionListener;
    }

    public void setSearchingListener(SearchListener searchListener) {
        this.searchListener = searchListener;
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
        Log.d(TAG, "Підключення до: " + device.getName());

        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        isConnected = false;
        isReadyToSend = false;
        dataCharacteristic = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
        }
    }

    public void sendCommand(String command) {
        if (!isReadyToSend || dataCharacteristic == null || bluetoothGatt == null) {
            Log.e(TAG, "Не готово до відправки");
            return;
        }

        String fullCommand = command + "\r\n";
        dataCharacteristic.setValue(fullCommand.getBytes());

        @SuppressLint("MissingPermission")
        boolean success = bluetoothGatt.writeCharacteristic(dataCharacteristic);

        if (success) {
            Log.d(TAG, "Команда відправлена: " + command);
        } else {
            Log.e(TAG, "Помилка відправки команди: " + command);
            attemptRecovery();
        }
    }

    @SuppressLint("MissingPermission")
    private void attemptRecovery() {
        Log.d(TAG, "Спроба відновлення з'єднання...");
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            bluetoothGatt.connect();
        }
    }

    public void turnDiodeOn() {
        sendCommand("turn_diode_on");
    }

    public void turnDiodeOff() {
        sendCommand("turn_diode_off");
    }

    public void sendHello() {
        sendCommand("hello");
    }

    public void requestStatus() {
        sendCommand("get_status");
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            Log.d(TAG, "Відключення");
        }
        isConnected = false;
        isReadyToSend = false;
        dataCharacteristic = null;
    }

    public boolean isReadyToSend() {
        return isReadyToSend;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // ----------------------------
    // BLE Callback
    // ----------------------------

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Підключено до BLE пристрою" + status + newState);
                isConnected = true;

                @SuppressLint("MissingPermission")
                boolean discoveryStarted = gatt.discoverServices();
                Log.d(TAG, "Початок пошуку сервісів: " + discoveryStarted);


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Відключено від BLE пристрою");
                isConnected = false;
                isReadyToSend = false;
                dataCharacteristic = null;
                connectionListener.onConnection();

                if (bluetoothGatt != null) {
                    Log.d(TAG, "Спроба перепідключення...");
                    bluetoothGatt.connect();
                }
            }
            mainHandler.post(()-> connectionListener.onConnection());

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Сервіси знайдено");
                setupDataCharacteristic(gatt);
            } else {
                Log.e(TAG, "❌ Помилка пошуку сервісів: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Готово до обміну даними");
                isReadyToSend = true;

                new android.os.Handler().postDelayed(() -> {
                    sendCommand("hello");
                }, 1000);

                mainHandler.post(()-> connectionListener.onConnection());

            } else {
                Log.e(TAG, "Помилка налаштування дескриптора: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Дані успішно відправлені до пристрою");
            } else {
                Log.e(TAG, "❌ Помилка відправки даних: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            String message = new String(data).trim();
            Log.d(TAG, "Отримано від модуля: " + message);
            connectionListener.onMessageFromDevice(message);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU змінено на: " + mtu);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void setupDataCharacteristic(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(HM_SERVICE_UUID);

        if (service == null) {
            Log.e(TAG, "❌ Сервіс FFE0 не знайдено!");
            for (BluetoothGattService s : gatt.getServices()) {
                Log.d(TAG, "Доступний сервіс: " + s.getUuid());
            }
            return;
        }

        dataCharacteristic = service.getCharacteristic(HM_CHARACTERISTIC_UUID);

        if (dataCharacteristic == null) {
            Log.e(TAG, "❌ Характеристика FFE1 не знайдено!");
            for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                Log.d(TAG, "Доступна характеристика: " + ch.getUuid() +
                        ", властивості: " + ch.getProperties());
            }
            return;
        }

        Log.d(TAG, "✅ Знайдено характеристику для даних");
        Log.d(TAG, "Властивості характеристики: " + dataCharacteristic.getProperties());

        int properties = dataCharacteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 &&
                (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            Log.e(TAG, "❌ Характеристика не підтримує запис!");
            return;
        }

        boolean notificationSet = gatt.setCharacteristicNotification(dataCharacteristic, true);
        Log.d(TAG, "Сповіщення увімкнено: " + notificationSet);

        BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            Log.d(TAG, "Налаштування дескриптора...");
        } else {
            Log.e(TAG, "❌ Дескриптор не знайдено!");
            isReadyToSend = true;
        }
    }



    // ----------------------------
    // Сканування пристроїв
    // ----------------------------

    @SuppressLint("MissingPermission")
    public boolean startDiscovery() {
        if (!hasBluetoothScanPermission()) {
            Log.e(TAG, "Немає дозволу на сканування");
            return false;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothReceiver, filter);

        if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
            visibleDevices.clear();
            boolean started = bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Сканування запущено: " + started);
            return started;
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public void cancelDiscovery() {
        if (hasBluetoothScanPermission() && bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Сканування зупинено");
        }
    }

    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (hasBluetoothConnectPermission() && bluetoothAdapter != null) {
            devices.addAll(bluetoothAdapter.getBondedDevices());
        }
        return devices;
    }

    public List<BluetoothDevice> getVisibleDevices() {
        return Collections.unmodifiableList(visibleDevices);
    }

    public void clearVisibleDevices() {
        visibleDevices.clear();
    }

    // ----------------------------
    // Дозволи
    // ----------------------------

    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public boolean hasRequiredPermissions() {
        return hasBluetoothConnectPermission() && hasBluetoothScanPermission();
    }

    // ----------------------------
    // Broadcast Receiver
    // ----------------------------

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null && !visibleDevices.contains(device)) {
                    visibleDevices.add(device);
                    Log.d(TAG, "📡 Знайдено: " + device.getName() + " (" + device.getAddress() + ")");

                    if (searchListener != null) {
                        searchListener.onDeviceFound(device);
                    }
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "🔍 Сканування завершено. Знайдено пристроїв: " + visibleDevices.size());
                context.unregisterReceiver(this);

                if (searchListener != null) {
                    searchListener.onDiscoveryFinished(visibleDevices.size());
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "🔍 Сканування розпочато");

                if (searchListener != null) {
                    searchListener.onDiscoveryStarted();
                }
            }
        }
    };

    // ----------------------------
    // Інтерфейси
    // ----------------------------

    public interface SearchListener {
        void onDeviceFound(BluetoothDevice device);
        void onDiscoveryStarted();
        void onDiscoveryFinished(int devicesCount);
    }

    public interface ConnectionListener{
        void onConnection();
        void onMessageFromDevice(String s);
    }
}