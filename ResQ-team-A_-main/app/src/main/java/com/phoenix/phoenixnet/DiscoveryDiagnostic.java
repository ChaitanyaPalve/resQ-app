package com.phoenix.phoenixnet;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

public class DiscoveryDiagnostic {
    private static final String TAG = "PheonixNetDiagnostic";

    public static String run(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=== HARDWARE DIAGNOSTIC ===\n");
        
        // 1. Bluetooth Low Energy
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        
        if (adapter == null) {
            report.append("BLE: NOT SUPPORTED\n");
        } else {
            report.append("BLE Enabled: ").append(adapter.isEnabled()).append("\n");
            report.append("BLE Adv Support: ").append(adapter.isMultipleAdvertisementSupported()).append("\n");
            if (!adapter.isEnabled()) report.append("ACTION: ENABLE BLUETOOTH\n");
        }

        // 2. Wi-Fi & Network
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            report.append("WiFi Enabled: ").append(wifi.isWifiEnabled()).append("\n");
            android.net.wifi.WifiInfo info = wifi.getConnectionInfo();
            int ip = info.getIpAddress();
            String ipStr = String.format(java.util.Locale.US, "%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            report.append("IP Address: ").append(ipStr).append("\n");
            if (!wifi.isWifiEnabled()) report.append("ACTION: ENABLE WIFI\n");
        }

        // 3. System Location (Crucial for BLE Scanning)
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            report.append("GPS Hardware: ").append(gpsEnabled).append("\n");
            if (!gpsEnabled) {
                report.append("CRITICAL: GPS OFF - BLE/WiFi SCANNING BLOCKED BY OS\n");
            }
        }

        // 4. Permissions
        boolean locPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        report.append("Loc Permission: ").append(locPerm).append("\n");

        Log.i(TAG, report.toString());
        return report.toString();
    }
}
