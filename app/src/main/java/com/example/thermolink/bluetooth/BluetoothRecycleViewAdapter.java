package com.example.thermolink.bluetooth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thermolink.R;

import java.util.List;

public class BluetoothRecycleViewAdapter extends RecyclerView.Adapter<BluetoothRecycleViewAdapter.BluetoothViewHolder> {

    private List<String> items; // список даних

    // Конструктор
    public BluetoothRecycleViewAdapter(List<String> items) {
        this.items = items;
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

        holder.textView.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
