package com.phoenix.phoenixnet;

import android.content.Context;
import android.content.SharedPreferences;
import com.phoenix.phoenixnet.db.AppDatabase;
import com.phoenix.phoenixnet.db.MeshPacketEntity;
import com.phoenix.phoenixnet.mesh.MeshPacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class PacketStoreAndForwardManager {
    private static final String PREF_NAME = "pheonix_dtn_store";
    private static final String SEEN_MSGS_KEY = "seen_message_ids";
    private static final int MAX_SEEN_CACHE_SIZE = 500;
    
    private final SharedPreferences prefs;
    private final Set<String> seenMessageIds;
    private final Context context;
    private final ExecutorService dbExecutor;
    private volatile boolean isDtnPaused = false;
    
    private final LinkedHashMap<String, Boolean> lruCache = new LinkedHashMap<String, Boolean>(MAX_SEEN_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_SEEN_CACHE_SIZE;
        }
    };

    public interface DtnEventListener {
        void onNewPacketStored(MeshPacket packet);
        void onPacketRelayed(MeshPacket packet);
    }

    public PacketStoreAndForwardManager(Context context, ExecutorService dbExecutor) {
        this.context = context;
        this.dbExecutor = dbExecutor;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.seenMessageIds = new HashSet<>(prefs.getStringSet(SEEN_MSGS_KEY, new HashSet<>()));
        for (String id : seenMessageIds) {
            lruCache.put(id, true);
        }
        // purgeExpiredPackets();
    }

    public synchronized boolean isDuplicate(String msgId) {
        if (lruCache.containsKey(msgId)) return true;
        if (seenMessageIds.contains(msgId)) {
            lruCache.put(msgId, true);
            return true;
        }
        return false;
    }

    public synchronized boolean processIncomingPacket(String rawString, DtnEventListener listener) {
        if (isDtnPaused) return false;
        // Simplified for Phase 1/2 parity - will be replaced by full MeshPacket serialization
        return false;
    }

    public synchronized MeshPacket createLocalSos(double lat, double lon, String sender, String details) {
        // This will now be handled via the BlockvoiceViewModel/Entity pipeline
        return null;
    }

    public synchronized List<MeshPacket> getPacketsForSync() {
        return new ArrayList<>();
    }

    public int getStoredCount() {
        int count = 0;
        for (String key : prefs.getAll().keySet()) if (key.startsWith("pkt_")) count++;
        return count;
    }

    public void setDtnPaused(boolean paused) {
        this.isDtnPaused = paused;
    }
}
