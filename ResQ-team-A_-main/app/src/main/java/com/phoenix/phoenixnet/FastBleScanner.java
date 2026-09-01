package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.Collections;
import java.util.UUID;

public class FastBleScanner {
    private static final String TAG = "FastBleScanner";
    private final BluetoothLeScanner scanner;
    private final UUID serviceUuid;
    private final DiscoveryCallback callback;

    public interface DiscoveryCallback {
        void onPeerFound(BluetoothDevice device, int rssi, String nodeId);
    }

    public FastBleScanner(BluetoothAdapter adapter, UUID serviceUuid, DiscoveryCallback callback) {
        this.scanner = adapter.getBluetoothLeScanner();
        this.serviceUuid = serviceUuid;
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (scanner == null) return;

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUuid))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        Log.d(TAG, "Aggressive BLE Scanner Started");
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        if (scanner != null) scanner.stopScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            byte[] manufacturerData = result.getScanRecord() != null ? 
                    result.getScanRecord().getManufacturerSpecificData(0xFEFF) : null;
            String nodeId = manufacturerData != null ? new String(manufacturerData) : "Unknown";
            
            Log.i(TAG, "PEER DISCOVERED: " + device.getAddress() + " | Node: " + nodeId + " | RSSI: " + result.getRssi());
            
            if (callback != null) {
                callback.onPeerFound(device, result.getRssi(), nodeId);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed: " + errorCode);
        }
    };
}
