package com.phoenix.phoenixnet;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MeshConnectionManager {
    private static final String TAG = "MeshConnectionManager";
    private final List<BluetoothSocket> btSockets = new CopyOnWriteArrayList<>();
    private final List<WifiSocketWrapper> wifiSockets = new CopyOnWriteArrayList<>();

    public interface PacketListener {
        void onPacketReceived(String packet, String source);
    }

    private PacketListener packetListener;

    public void setPacketListener(PacketListener listener) {
        this.packetListener = listener;
    }

    public synchronized void addBluetoothSocket(BluetoothSocket socket) {
        if (socket == null) return;
        btSockets.add(socket);
        new BluetoothReadThread(socket).start();
        Log.d(TAG, "Bluetooth peer added: " + socket.getRemoteDevice().getAddress());
    }

    public synchronized void addWifiSocket(java.net.Socket socket) {
        if (socket == null) return;
        WifiSocketWrapper wrapper = new WifiSocketWrapper(socket);
        wifiSockets.add(wrapper);
        new WifiReadThread(wrapper).start();
        Log.d(TAG, "Wi-Fi peer added: " + socket.getInetAddress());
    }

    public void broadcast(String packet) {
        byte[] data = (packet + "\n").getBytes();
        
        // Broadcast to BT pool
        for (BluetoothSocket socket : btSockets) {
            try {
                OutputStream os = socket.getOutputStream();
                os.write(data);
                os.flush();
            } catch (IOException e) {
                btSockets.remove(socket);
                Log.e(TAG, "BT peer disconnected during broadcast");
            }
        }

        // Broadcast to Wi-Fi pool
        for (WifiSocketWrapper wrapper : wifiSockets) {
            try {
                wrapper.out.write(data);
                wrapper.out.flush();
            } catch (IOException e) {
                wifiSockets.remove(wrapper);
                Log.e(TAG, "Wi-Fi peer disconnected during broadcast");
            }
        }
    }

    /**
     * Target specific peer for high-priority Flash SOS
     */
    public void sendTargetedFlashSos(Object packetObj) { // Changed MeshPacket to Object temporarily
        /*
        MeshPacket packet = (MeshPacket) packetObj;
        String serialized = packet.serialize() + "\n";
        byte[] data = serialized.getBytes();
        boolean sent = false;

        // Try BT pool
        for (BluetoothSocket socket : btSockets) {
            if (socket.getRemoteDevice().getAddress().equals(packet.targetNodeId)) {
                try {
                    socket.getOutputStream().write(data);
                    socket.getOutputStream().flush();
                    sent = true;
                    Log.d(TAG, "Flash SOS sent to BT peer: " + packet.targetNodeId);
                } catch (IOException e) {
                    btSockets.remove(socket);
                }
            }
        }

        // Try Wi-Fi pool (assuming targetNodeId matches InetAddress or a mapped UUID)
        for (WifiSocketWrapper wrapper : wifiSockets) {
            if (wrapper.socket.getInetAddress().toString().contains(packet.targetNodeId)) {
                try {
                    wrapper.out.write(data);
                    wrapper.out.flush();
                    sent = true;
                    Log.d(TAG, "Flash SOS sent to Wi-Fi peer: " + packet.targetNodeId);
                } catch (IOException e) {
                    wifiSockets.remove(wrapper);
                }
            }
        }

        if (!sent) {
            Log.w(TAG, "Target peer not found in pool, fallback to broadcast");
            broadcast(packet.serialize());
        }
        */
    }

    private class BluetoothReadThread extends Thread {
        private final BluetoothSocket socket;
        public BluetoothReadThread(BluetoothSocket socket) { this.socket = socket; }
        public void run() {
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (packetListener != null) packetListener.onPacketReceived(line, "BT");
                }
            } catch (IOException e) {
                btSockets.remove(socket);
            }
        }
    }

    private class WifiReadThread extends Thread {
        private final WifiSocketWrapper wrapper;
        public WifiReadThread(WifiSocketWrapper wrapper) { this.wrapper = wrapper; }
        public void run() {
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(wrapper.in));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (packetListener != null) packetListener.onPacketReceived(line, "WiFi");
                }
            } catch (IOException e) {
                wifiSockets.remove(wrapper);
            }
        }
    }

    private static class WifiSocketWrapper {
        java.net.Socket socket;
        java.io.InputStream in;
        java.io.OutputStream out;
        public WifiSocketWrapper(java.net.Socket s) {
            this.socket = s;
            try {
                in = s.getInputStream();
                out = s.getOutputStream();
            } catch (IOException ignored) {}
        }
    }
}
