package com.example.thermolink;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.thermolink.bluetooth.MyBluetoothHelper;

import org.w3c.dom.Text;


public class Device_converstation extends Fragment {
    public final static Float REFERENCE_VOLTAGE = 2.95F;
    private MyBluetoothHelper bluetoothHelper;
    private TextView isConnected_tv, messageFromDevice_tv, ambient_temp_tv, object_temp_tv, tv_temp_thermopair, tv_voltage_thermopair, tv_raw_thermopair;
    private BluetoothDevice selectedDevice;
    private Button btn_pirometr_info, btn_turOnDiode, btn_thermopair_info;

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

        ambient_temp_tv = view.findViewById(R.id.tv_ambient_pirometer);
        object_temp_tv = view.findViewById(R.id.tv_object_pirometer);

        tv_temp_thermopair = view.findViewById(R.id.tv_temp_thermopair);
        tv_raw_thermopair = view.findViewById(R.id.tv_raw_thermopair);
        tv_voltage_thermopair = view.findViewById(R.id.tv_voltage_thermopair);

        btn_turOnDiode = view.findViewById(R.id.btn_turn_on_diode);
        btn_turOnDiode.setOnClickListener(v -> {
            bluetoothHelper.sendCommand("SWITCH_LED");
        });

        btn_pirometr_info = view.findViewById(R.id.btn_pirometr_info);
        btn_pirometr_info.setOnClickListener(v -> bluetoothHelper.sendCommand("PIROMETR"));

        btn_thermopair_info = view.findViewById(R.id.btn_thermopair_info);
        btn_thermopair_info.setOnClickListener(v -> bluetoothHelper.sendCommand("THERMOPAIR"));



        bluetoothHelper.setConnectionListener(new MyBluetoothHelper.ConnectionListener() {
            @Override
            public void onConnection() {
                isConnected_tv.setText("Is connected: " + bluetoothHelper.isConnected());

            }

            @Override
            public void onMessageFromDevice(String s) {
                messageFromDevice_tv.setText("Message from device: " + s);
                messageFromDeviceHandler(s);
            }
        });


        return view;
    }

    @SuppressLint("SetTextI18n")
    private void messageFromDeviceHandler(String s){
        Handler mainHandler  = new Handler(Looper.getMainLooper());

        if (s.startsWith("pirometr")){
            s = s.substring(8);
            String[] parts = s.split("\\|");
            final String ambientText = parts[0] + " °C";
            final String objectText = parts[1] + " °C";
            mainHandler.post(() -> {
                ambient_temp_tv.setText(ambientText);
                object_temp_tv.setText(objectText);
            });
        } else if (s.startsWith("thermopair")) {
            s = s.substring(10);
            final String raw_text = s;
            final float voltage_thermopair = (Integer.parseInt(s) * REFERENCE_VOLTAGE) / 4096.0f;
            @SuppressLint("DefaultLocale") final String voltageText = String.format("%.4f V", voltage_thermopair);
            @SuppressLint("DefaultLocale")
            final String temperature_text = String.format("%.3f °C", (voltage_thermopair - 0.0615f) * 100f);

            mainHandler.post(()->{
                tv_raw_thermopair.setText(raw_text);
                tv_temp_thermopair.setText(temperature_text);
                tv_voltage_thermopair.setText(voltageText);
            });
        }
    }

}