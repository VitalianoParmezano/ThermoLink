package com.example.thermolink.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thermolink.Bluetooth_fragment;
import com.example.thermolink.R;

import java.util.List;

public class BluetoothRecycleViewAdapter extends RecyclerView.Adapter<BluetoothRecycleViewAdapter.BluetoothViewHolder> {
    private final String TAG = "Bluetooth Recyclerview";
    Bluetooth_fragment.OnDeviceClickListener onDeviceClickListener;
    private List<BluetoothDevice> items; // список даних
    private MyBluetoothHelper bluetoothHelper;

    // Конструктор
    public BluetoothRecycleViewAdapter(List<BluetoothDevice> items, Bluetooth_fragment.OnDeviceClickListener listener, Context context) {
        this.items = items;
        this.onDeviceClickListener = listener;
        bluetoothHelper = MyBluetoothHelper.getInstance(context);

    }

    // ViewHolder
    public static class BluetoothViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public BluetoothViewHolder(@NonNull View view) {
            super(view);
            textView = view.findViewById(R.id.item_device_name);
        }
    }

    @NonNull
    @Override
    public BluetoothViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_list, parent, false);
        return new BluetoothViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothViewHolder holder, int position) {
        // Встановлюємо текст з нашого списку
        holder.textView.setText(items.get(position).getName());
        holder.itemView.setOnClickListener(v -> {
            onDeviceClickListener.onDeviceClick(items.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}

