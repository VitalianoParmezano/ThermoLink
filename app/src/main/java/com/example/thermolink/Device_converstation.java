package com.example.thermolink;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.thermolink.bluetooth.MyBluetoothHelper;

import org.w3c.dom.Text;


public class Device_converstation extends Fragment {
    private MyBluetoothHelper bluetoothHelper;
    private TextView isConnected_tv, messageFromDevice_tv;
    private BluetoothDevice selectedDevice;
    private Button btn_debug, btn_turOnDiode;

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

        messageFromDevice_tv = view.findViewById(R.id.tv_receive);
        isConnected_tv = view.findViewById(R.id.tv_is_connected);

        btn_turOnDiode = view.findViewById(R.id.btn_turn_on_diode);
        btn_turOnDiode.setOnClickListener(v -> {
            bluetoothHelper.sendCommand("SWITCH_LED");
        });



        bluetoothHelper.setConnectionListener(new MyBluetoothHelper.ConnectionListener() {
            @Override
            public void onConnection() {
                isConnected_tv.setText("Is connected: " + bluetoothHelper.isConnected());

            }

            @Override
            public void onMessageFromDevice(String s) {
                messageFromDevice_tv.setText("Message from device: " + s);
            }
        });


        return view;
    }

}