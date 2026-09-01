package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WifiDirectManager {
    private static final String TAG = "WifiDirectManager";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final DiscoveryCallback callback;

    public interface DiscoveryCallback {
        void onPeerFound(String nodeId, String deviceAddress);
    }

    public WifiDirectManager(Context context, String nodeId, DiscoveryCallback callback) {
        this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(context, context.getMainLooper(), null);
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { Log.d(TAG, "Discovery Started"); }
            @Override
            public void onFailure(int reason) { Log.e(TAG, "Discovery Failed: " + reason); }
        });
    }

    public void stop() {
        manager.stopPeerDiscovery(channel, null);
    }
}
