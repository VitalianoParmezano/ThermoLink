package com.example.thermolink;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.thermolink.bluetooth.MyBluetoothHelper;


public class Device_converstation extends Fragment {
    MyBluetoothHelper bluetoothHelper;
    BluetoothDevice selectedDevice;
    Button btn_debug, btn_turOnDiode;

    public Device_converstation() {}

    public static Device_converstation newInstance (BluetoothDevice device){
        Device_converstation fragment = new Device_converstation();
        Bundle args = new Bundle();
        args.putParcelable("selected_device",device);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedDevice = getArguments().getParcelable("selected_device");
        }
        bluetoothHelper = MyBluetoothHelper.getInstance(getContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_converstation, container, false);

        btn_debug = view.findViewById(R.id.btn_debug);

        btn_debug.setOnClickListener(v -> {
            bluetoothHelper.connect(selectedDevice);
        });

        btn_turOnDiode = view.findViewById(R.id.btn_turn_on_diode);
        btn_turOnDiode.setOnClickListener(v -> {
            bluetoothHelper.sendCommand("wow");
        });

        return view;
    }
}