package com.phoenix.phoenixnet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MeshMapManager {
    private static final String TAG = "MeshMapManager";
    private final Context context;
    private final MapView mapView;
    private final TextView tvStatusBadge;
    
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private final Map<String, Marker> activeMarkers = new HashMap<>();

    public MeshMapManager(Context context, MapView mapView, TextView tvStatusBadge) {
        this.context = context;
        this.mapView = mapView;
        this.tvStatusBadge = tvStatusBadge;
        initialize();
    }

    private void initialize() {
        // 1. OSMDroid Configuration
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        File osmdroidDir = new File(context.getFilesDir(), "osmdroid");
        if (!osmdroidDir.exists()) osmdroidDir.mkdirs();
        Configuration.getInstance().setOsmdroidBasePath(context.getFilesDir());
        Configuration.getInstance().setOsmdroidTileCache(new File(osmdroidDir, "tiles"));

        // 2. Base Map Setup
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setUseDataConnection(true); // Hybrid mode: true, but will fallback to cache

        // 3. Load Offline Archives if present
        loadOfflineArchives(osmdroidDir);

        // 4. Overlays: GPS & Compass
        setupOverlays();
        
        updateStatusBadge();
    }

    private void loadOfflineArchives(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        java.util.List<File> archives = new ArrayList<>();
        for (File file : files) {
            if (file.getName().endsWith(".mbtiles") || file.getName().endsWith(".sqlite")) {
                archives.add(file);
            }
        }

        if (!archives.isEmpty()) {
            try {
                OfflineTileProvider provider = new OfflineTileProvider(new org.osmdroid.tileprovider.util.SimpleRegisterReceiver(context), archives.toArray(new File[0]));
                mapView.setTileProvider(provider);
                Log.d(TAG, "Loaded " + archives.size() + " offline archives");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load archives", e);
            }
        }
    }

    private void setupOverlays() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        compassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
    }

    public void updateStatusBadge() {
        if (tvStatusBadge == null) return;
        boolean isOffline = !mapView.useDataConnection();
        tvStatusBadge.setText(isOffline ? "Map: Offline (Cache)" : "Map: Online (Live)");
        tvStatusBadge.setBackgroundColor(isOffline ? Color.parseColor("#80FF5722") : Color.parseColor("#804CAF50"));
    }

    public synchronized void plotMeshPacket(Object packetObj) { // Changed MeshPacket to Object and method name from plotDistressPacket temporarily
        /*
        MeshPacket packet = (MeshPacket) packetObj;
        if (packet == null || packet.msgId == null) return;

        GeoPoint point = new GeoPoint(packet.lat, packet.lon);
        Marker marker = activeMarkers.get(packet.senderName); // Deduplicate by sender for node tracking

        if (marker == null) {
            marker = new Marker(mapView);
            activeMarkers.put(packet.senderName, marker);
            mapView.getOverlays().add(marker);
        }

        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Node: " + packet.senderName);
        
        String snippet = "Priority: " + packet.priority + "\n" +
                         "Hops: " + packet.hopCount + "\n" +
                         "Msg: " + packet.payload + "\n" +
                         "Time: " + new java.util.Date(packet.timestamp).toString();
        marker.setSnippet(snippet);

        // Styling based on Priority
        Drawable icon = ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_map);
        if (icon != null) {
            icon = icon.mutate();
            int color;
            switch (packet.priority) {
                case 1: color = Color.RED; break;
                case 2: color = Color.parseColor("#FFA500"); break; // Orange
                default: color = Color.BLUE; break;
            }
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            marker.setIcon(icon);
        }

        // Add Pulsing Circle for Priority 1 (SOS)
        if (packet.priority == 1) {
            Polygon circle = new Polygon();
            circle.setPoints(Polygon.pointsAsCircle(point, 100)); // 100 meters radius
            circle.getFillPaint().setColor(Color.argb(50, 255, 0, 0));
            circle.getOutlinePaint().setColor(Color.RED);
            circle.getOutlinePaint().setStrokeWidth(2.0f);
            mapView.getOverlays().add(circle);
        }

        mapView.invalidate();
        */
    }

    public GeoPoint getCurrentLocation() {
        return myLocationOverlay != null ? myLocationOverlay.getMyLocation() : null;
    }

    public void centerOnLocation() {
        if (myLocationOverlay != null) {
            myLocationOverlay.enableFollowLocation();
            GeoPoint myLoc = myLocationOverlay.getMyLocation();
            if (myLoc != null) {
                mapView.getController().animateTo(myLoc);
            }
        }
    }

    public void onResume() {
        mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        if (compassOverlay != null) compassOverlay.enableCompass();
    }

    public void onPause() {
        mapView.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        if (compassOverlay != null) compassOverlay.disableCompass();
    }
}
