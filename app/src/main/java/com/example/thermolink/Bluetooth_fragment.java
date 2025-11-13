package com.example.thermolink;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.thermolink.bluetooth.BluetoothRecycleViewAdapter;
import com.example.thermolink.bluetooth.MyBluetoothHelper;

import java.util.ArrayList;
import java.util.List;

public class Bluetooth_fragment extends Fragment {
    private OnDeviceClickListener onDeviceClickListener;
RecyclerView bluetoothRecycleView;
BluetoothRecycleViewAdapter adapter;


    public Bluetooth_fragment() {
        // Required empty public constructor
    }

    //    public static Bluetooth_fragment newInstance(OnDeviceClickListener listener) {
    //        Bluetooth_fragment fragment = new Bluetooth_fragment();
    //        Bundle args = new Bundle();
    //        args.
    //        fragment.setArguments(args);
    //        return fragment;
    //    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDeviceClickListener){
            onDeviceClickListener = (OnDeviceClickListener) context;
        } else {
            throw new RuntimeException(context + "must implement onDeviceClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onDeviceClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

        }

        MyBluetoothHelper bluetoothHelper = MyBluetoothHelper.getInstance(getContext());
        bluetoothHelper.setConnectionListener((device, isConnected) -> {
            adapter.notifyDataSetChanged();
        });

        List<BluetoothDevice> deviceList = bluetoothHelper.getVisibleDevices();

        adapter = new BluetoothRecycleViewAdapter(deviceList, onDeviceClickListener, getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment


        View view = inflater.inflate(R.layout.fragment_bluetooth_fragment, container, false);

        bluetoothRecycleView = view.findViewById(R.id.bluetooth_recyclerview);
        bluetoothRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));

        bluetoothRecycleView.setAdapter(adapter);


        return view;
    }

    public interface OnDeviceClickListener{
        void onDeviceClick(BluetoothDevice device);
    }

}