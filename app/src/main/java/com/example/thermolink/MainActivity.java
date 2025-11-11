package com.example.thermolink;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.thermolink.bluetooth.MyBluetoothHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MyBluetoothHelper bluetoothHelper;
    TextView debug_tv;
    private List<String> deviceNames = new ArrayList<>();
    ImageButton update_device_list_btn;
    FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION

        }, 1);

        bluetoothHelper = MyBluetoothHelper.getInstance(this);

        frameLayout = findViewById(R.id.fragment_container);
        update_device_list_btn = findViewById(R.id.update_device_lists);
        debug_tv = findViewById(R.id.debug_tv);

        update_device_list_btn.setOnClickListener(v -> {
            bluetoothHelper.startDiscovery();

            List<BluetoothDevice> test = bluetoothHelper.getVisibleDevices();
            if (test.isEmpty()) {
                return;
            }
            debug_tv.setText(test.get(0).getName());
        });

        if (savedInstanceState == null) {
            addInitialFragment();
        }

    }

    private void addInitialFragment() {
        Bluetooth_fragment fragment = new Bluetooth_fragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }



}
