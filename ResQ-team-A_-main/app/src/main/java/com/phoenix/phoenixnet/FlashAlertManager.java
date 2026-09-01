package com.phoenix.phoenixnet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import org.osmdroid.util.GeoPoint;

public class FlashAlertManager {
    private final Activity activity;
    private final MeshMapManager mapManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog activeDialog;

    public FlashAlertManager(Activity activity, MeshMapManager mapManager) {
        this.activity = activity;
        this.mapManager = mapManager;
    }

    public void triggerFlashAlert(Object packetObj) { // Changed MeshPacket to Object temporarily
        /*
        MeshPacket packet = (MeshPacket) packetObj;
        handler.post(() -> {
            // 1. Wake screen
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON 
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON 
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

            // 2. Play Sound
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000);

            // 3. Vibrate
            Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                long[] pattern = {0, 500, 200, 500};
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }

            // 4. Show Pulsing UI
            showEmergencyDialog(packet);

            // 5. Center Map
            if (mapManager != null) {
                GeoPoint point = new GeoPoint(packet.lat, packet.lon);
                mapManager.plotMeshPacket(packet);
                mapManager.enableFollowLocation(); // Assuming this centers as well
            }
        });
        */
    }

    private void showEmergencyDialog(Object packetObj) { // Changed MeshPacket to Object temporarily
        /*
        MeshPacket packet = (MeshPacket) packetObj;
        if (activeDialog != null) activeDialog.dismiss();

        View dialogView = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        TextView tv = dialogView.findViewById(android.R.id.text1);
        tv.setText("!!! FLASH SOS !!!\nSender: " + packet.senderName + "\nPayload: " + packet.payload);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(24);
        tv.setBackgroundColor(Color.RED);

        activeDialog = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                .setView(dialogView)
                .setPositiveButton("ACKNOWLEDGE", (d, w) -> {
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    d.dismiss();
                })
                .setCancelable(false)
                .create();

        activeDialog.show();
        
        // Pulsing Effect
        handler.postDelayed(new Runnable() {
            boolean red = true;
            @Override
            public void run() {
                if (activeDialog != null && activeDialog.isShowing()) {
                    tv.setBackgroundColor(red ? Color.BLACK : Color.RED);
                    red = !red;
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
        */
    }
}
