package com.example.thermolink;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.thermolink.bluetooth.MyBluetoothHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Bluetooth_fragment.OnDeviceClickListener {
    private MyBluetoothHelper bluetoothHelper;
    private TextView debug_tv;
    private List<String> deviceNames = new ArrayList<>();
    private ImageButton update_device_list_btn;
    private FrameLayout frameLayout;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;

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
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN

        }, 1);

        bluetoothHelper = MyBluetoothHelper.getInstance(this);

        //drawerLayout = findViewById(R.id.main);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        frameLayout = findViewById(R.id.fragment_container);
        update_device_list_btn = findViewById(R.id.update_device_lists);

        update_device_list_btn.setOnClickListener(v -> {
            bluetoothHelper.startDiscovery();

            List<BluetoothDevice> test = bluetoothHelper.getVisibleDevices();
            if (test.isEmpty()) {
                return;
            }
        });

        if (savedInstanceState == null) {
            addInitialFragment();
        }



        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new Bluetooth_fragment();
            } else if (itemId == R.id.nav_info) {
                selectedFragment = new Device_converstation();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
            return true;
        });
    }

    private void addInitialFragment() {
        Bluetooth_fragment fragment = new Bluetooth_fragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }


    @Override
    public void onDeviceClick(BluetoothDevice device) {
        Fragment selectedFragment = Device_converstation.newInstance(device);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        bluetoothHelper.connect(device);

        bottomNavigationView.setSelectedItemId(R.id.nav_info);
    }
}
