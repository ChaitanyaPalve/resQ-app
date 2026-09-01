package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

public class WifiDirectReceiver extends BroadcastReceiver {
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final PeerListListener listener;

    public interface PeerListListener {
        void onPeersAvailable(WifiP2pDeviceList peers);
        void onConnected(WifiP2pInfo info);
        void onDeviceInfoAvailable(WifiP2pDevice device);
    }

    public WifiDirectReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, PeerListListener listener) {
        this.manager = manager;
        this.channel = channel;
        this.listener = listener;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, peers -> {
                    if (listener != null) listener.onPeersAvailable(peers);
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) return;
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, info -> {
                    if (listener != null) listener.onConnected(info);
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            if (listener != null) {
                listener.onDeviceInfoAvailable(device);
            }
        }
    }
}
