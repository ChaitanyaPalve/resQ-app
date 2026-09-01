package com.phoenix.phoenixnet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpMeshSocket {
    private static final int PORT = 8888;
    private DatagramSocket socket;
    private boolean isRunning = false;

    public interface UdpPacketListener {
        void onPacketReceived(String data);
    }

    public void startListening(UdpPacketListener listener) {
        isRunning = true;
        new Thread(() -> {
            try {
                socket = new DatagramSocket(PORT);
                socket.setBroadcast(true);
                byte[] buffer = new byte[2048];

                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    if (listener != null) {
                        listener.onPacketReceived(msg);
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public void broadcastPacket(String message) {
        new Thread(() -> {
            try {
                DatagramSocket txSocket = new DatagramSocket();
                txSocket.setBroadcast(true);
                byte[] data = message.getBytes();
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddr, PORT);
                txSocket.send(packet);
                txSocket.close();
            } catch (Exception ignored) {}
        }).start();
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
