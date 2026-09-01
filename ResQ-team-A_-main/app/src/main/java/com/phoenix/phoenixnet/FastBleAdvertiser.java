package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.UUID;

public class FastBleAdvertiser {
    private static final String TAG = "FastBleAdvertiser";
    private final BluetoothLeAdvertiser advertiser;
    private final UUID serviceUuid;
    private final String nodeId;

    public FastBleAdvertiser(BluetoothAdapter adapter, UUID serviceUuid, String nodeId) {
        this.advertiser = adapter.getBluetoothLeAdvertiser();
        this.serviceUuid = serviceUuid;
        this.nodeId = nodeId;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (advertiser == null) {
            Log.e(TAG, "Hardware Error: BluetoothLeAdvertiser is NULL (Maybe BLE is unsupported?)");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) // ~100ms interval
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        // Requirement 1: Primary packet (Keep under 31 bytes)
        // UUID (16 bytes) + Flags (3 bytes) = 19 bytes
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(serviceUuid))
                .setIncludeTxPowerLevel(false) // Crucial: Save space
                .build();

        // Move Name and NodeID to Scan Response (Extra 31 bytes)
        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addManufacturerData(0xFEFF, nodeId.getBytes()) 
                .build();

        try {
            advertiser.startAdvertising(settings, data, scanResponse, callback);
            Log.d(TAG, "Aggressive BLE Advertising Attempted: " + nodeId);
        } catch (Exception e) {
            Log.e(TAG, "Fatal error starting advertising: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        if (advertiser != null) advertiser.stopAdvertising(callback);
    }

    private final AdvertiseCallback callback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "BLE Beacon Active: Low Latency Mode");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            String reason;
            switch (errorCode) {
                case ADVERTISE_FAILED_DATA_TOO_LARGE: reason = "DATA_TOO_LARGE (>31 bytes)"; break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS: reason = "TOO_MANY_ADVERTISERS"; break;
                case ADVERTISE_FAILED_ALREADY_STARTED: reason = "ALREADY_STARTED"; break;
                case ADVERTISE_FAILED_INTERNAL_ERROR: reason = "INTERNAL_ERROR"; break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED: reason = "FEATURE_UNSUPPORTED"; break;
                default: reason = "UNKNOWN_" + errorCode;
            }
            Log.e(TAG, "BLE Beacon Failed: " + reason);
        }
    };
}
