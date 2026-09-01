package com.phoenix.phoenixnet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PeerDeviceAdapter extends RecyclerView.Adapter<PeerDeviceAdapter.ViewHolder> {

    public interface OnPeerActionListener {
        void onFlashSos(PeerDevice device);
    }

    private final List<PeerDevice> peerList = new ArrayList<>();
    private final OnPeerActionListener listener;

    public PeerDeviceAdapter(OnPeerActionListener listener) {
        this.listener = listener;
    }

    public synchronized void updatePeers(List<PeerDevice> newPeers) {
        peerList.clear();
        peerList.addAll(newPeers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.peer_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PeerDevice device = peerList.get(position);
        holder.tvName.setText(device.deviceName);
        holder.tvSignal.setText("Signal: " + device.rssi + " dBm");
        
        int iconRes = (device.transport == PeerDevice.Transport.BLUETOOTH) 
            ? android.R.drawable.stat_sys_data_bluetooth 
            : android.R.drawable.stat_sys_phone_call; // Placeholder for WiFi
        holder.ivIcon.setImageResource(iconRes);

        holder.btnFlash.setOnClickListener(v -> {
            if (listener != null) listener.onFlashSos(device);
        });
    }

    @Override
    public int getItemCount() {
        return peerList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSignal;
        ImageView ivIcon;
        Button btnFlash;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_peer_name);
            tvSignal = itemView.findViewById(R.id.tv_signal_info);
            ivIcon = itemView.findViewById(R.id.iv_transport_icon);
            btnFlash = itemView.findViewById(R.id.btn_flash_sos);
        }
    }
}
