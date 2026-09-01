package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;

public class IroncladDiscoveryManager {
    private static final String TAG = "IroncladDiscovery";
    private static final UUID MESH_UUID = UUID.fromString("7b34f0e0-afac-11de-8a39-0800200c9a66");

    private final FastBleAdvertiser advertiser;
    private final FastBleScanner scanner;
    private final UdpDiscoveryEngine udpEngine;
    private final WifiDirectManager wifiDirectManager;
    private final MeshConnectionManager connectionManager;
    private final DiscoveryListener uiListener;
    private InsecureAcceptThread acceptThread;

    public interface DiscoveryListener {
        void onPeerDetected(String id, String name, PeerDevice.Transport transport);
    }

    public IroncladDiscoveryManager(Context context, String myNodeId, MeshConnectionManager connectionManager, DiscoveryListener uiListener) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        this.connectionManager = connectionManager;
        this.uiListener = uiListener;

        if (adapter != null && adapter.isEnabled()) {
            this.advertiser = new FastBleAdvertiser(adapter, MESH_UUID, myNodeId);
            this.scanner = new FastBleScanner(adapter, MESH_UUID, (device, rssi, nodeId) -> {
                Log.i(TAG, "BLE Detected: " + device.getAddress() + " | RSSI: " + rssi + " | Node: " + nodeId);
                String name = "BLE Node";
                try {
                    name = device.getName();
                } catch (SecurityException ignored) {}
                if (uiListener != null) uiListener.onPeerDetected(device.getAddress(), name, PeerDevice.Transport.BLUETOOTH);
                autoConnect(device);
            });
        } else {
            this.advertiser = null;
            this.scanner = null;
            Log.e(TAG, "Bluetooth not available or disabled");
        }

        this.udpEngine = new UdpDiscoveryEngine(context, myNodeId, (nodeId, ip) -> {
            Log.i(TAG, "UDP Detected: " + nodeId + " at " + ip);
            if (uiListener != null) uiListener.onPeerDetected(nodeId, "Wi-Fi Node: " + nodeId, PeerDevice.Transport.WIFI_DIRECT);
        });

        this.wifiDirectManager = new WifiDirectManager(context, myNodeId, (nodeId, deviceAddress) -> {
            Log.i(TAG, "Wi-Fi Direct Detected: " + nodeId);
            if (uiListener != null) uiListener.onPeerDetected(deviceAddress, "P2P Node: " + nodeId, PeerDevice.Transport.WIFI_DIRECT);
        });

        if (adapter != null) {
            this.acceptThread = new InsecureAcceptThread(adapter);
        }
    }

    public void start() {
        if (advertiser != null) advertiser.start();
        if (scanner != null) scanner.start();
        if (acceptThread != null && !acceptThread.isAlive()) acceptThread.start();
        udpEngine.start();
        wifiDirectManager.startDiscovery();
    }

    public void stop() {
        if (advertiser != null) advertiser.stop();
        if (scanner != null) scanner.stop();
        if (acceptThread != null) acceptThread.interrupt();
        udpEngine.stop();
        wifiDirectManager.stop();
    }

    private class InsecureAcceptThread extends Thread {
        private android.bluetooth.BluetoothServerSocket serverSocket;
        private final BluetoothAdapter adapter;

        @SuppressLint("MissingPermission")
        public InsecureAcceptThread(BluetoothAdapter adapter) {
            this.adapter = adapter;
            try {
                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("PheonixNetInsecure", MESH_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Insecure server failed", e);
            }
        }

        public void run() {
            while (!isInterrupted()) {
                try {
                    if (serverSocket == null) break;
                    BluetoothSocket socket = serverSocket.accept();
                    if (socket != null) {
                        connectionManager.addBluetoothSocket(socket);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void autoConnect(BluetoothDevice device) {
        new Thread(() -> {
            try {
                // Zero-Prompt Insecure Socket
                BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(MESH_UUID);
                socket.connect();
                connectionManager.addBluetoothSocket(socket);
                Log.d(TAG, "Ironclad Auto-Connect Success: " + device.getAddress());
            } catch (IOException e) {
                // Silently fail if busy
            }
        }).start();
    }
}
