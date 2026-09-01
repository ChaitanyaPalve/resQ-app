package com.phoenix.phoenixnet;

import java.util.Objects;

public class PeerDevice {
    public enum Transport { BLUETOOTH, WIFI_DIRECT }

    public final String deviceId; // MAC address or UUID
    public final String deviceName;
    public final Transport transport;
    public int rssi;
    public long lastSeen;
    public Double latitude;
    public Double longitude;

    public PeerDevice(String deviceId, String deviceName, Transport transport, int rssi) {
        this.deviceId = deviceId;
        this.deviceName = deviceName != null ? deviceName : "Unknown Device";
        this.transport = transport;
        this.rssi = rssi;
        this.lastSeen = System.currentTimeMillis();
    }

    public void updateLocation(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
        this.lastSeen = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerDevice that = (PeerDevice) o;
        return Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId);
    }
}
