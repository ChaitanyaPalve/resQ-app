package com.phoenix.phoenixnet;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpDiscoveryEngine {
    private static final String TAG = "UdpDiscovery";
    private static final int PORT = 8889;
    private final String nodeId;
    private final DiscoveryCallback callback;
    private DatagramSocket socket;
    private boolean running;

    public interface DiscoveryCallback {
        void onNodeDetected(String id, String ip);
    }

    public UdpDiscoveryEngine(Context context, String nodeId, DiscoveryCallback callback) {
        this.nodeId = nodeId;
        this.callback = callback;
    }

    public void start() {
        running = true;
        new Thread(this::listen).start();
        new Thread(this::broadcast).start();
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    private void listen() {
        try {
            socket = new DatagramSocket(PORT);
            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength());
                if (data.startsWith("PHEONIX_NODE|") && !data.contains(nodeId)) {
                    String peerId = data.split("\\|")[1];
                    callback.onNodeDetected(peerId, packet.getAddress().getHostAddress());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Listen error: " + e.getMessage());
        }
    }

    private void broadcast() {
        while (running) {
            try {
                String msg = "PHEONIX_NODE|" + nodeId;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), PORT);
                if (socket != null) socket.send(packet);
                Thread.sleep(5000);
            } catch (Exception e) {
                Log.e(TAG, "Broadcast error: " + e.getMessage());
            }
        }
    }
}
