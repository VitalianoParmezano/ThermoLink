package com.example.thermolink;

import android.os.Bundle;

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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Bluetooth_fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Bluetooth_fragment extends Fragment {
RecyclerView bluetoothRecycleView;
BluetoothRecycleViewAdapter adapter;

    public Bluetooth_fragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static Bluetooth_fragment newInstance() {
        Bluetooth_fragment fragment = new Bluetooth_fragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

        }

        MyBluetoothHelper bluetoothHelper = new MyBluetoothHelper(getContext());

        ArrayList<String> deviceList = new ArrayList<>(bluetoothHelper.getPairedDevices());

        adapter = new BluetoothRecycleViewAdapter(deviceList);
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
}