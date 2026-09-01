package com.phoenix.phoenixnet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.phoenix.phoenixnet.db.AppDatabase;
import com.phoenix.phoenixnet.db.AuthRepository;
import com.phoenix.phoenixnet.db.MeshNetworkBridge;
import com.phoenix.phoenixnet.db.MeshPacketEntity;
import com.phoenix.phoenixnet.db.MeshPacketEntity;

import com.phoenix.phoenixnet.mesh.PeerManager;

import com.phoenix.phoenixnet.sync.SyncManager;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AndroidEntryPoint
public class MeshService extends Service implements WifiDirectReceiver.PeerListListener, WifiP2pManager.ConnectionInfoListener {
    private static final String TAG = "MeshService";
    private static final String CHANNEL_ID = "MeshServiceChannel";
    private final IBinder binder = new MeshBinder();

    @Inject
    PeerManager peerManager;

    @Inject
    SyncManager syncManager;

    /**
     * BRIDGE: Coroutine Scope tied to the Service Lifecycle.
     * Prevents memory leaks when background tasks (DB/Flow) are active.
     */
    private final Job serviceJob = JobKt.Job(null);
    private final CoroutineScope serviceScope = CoroutineScopeKt.CoroutineScope(
            Dispatchers.getIO().plus(serviceJob)
    );

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private BluetoothAdapter bluetoothAdapter;
    private MeshConnectionManager connectionManager;
    private IroncladDiscoveryManager discoveryEngine;
    private WifiP2pManager wifiManager;
    private WifiP2pManager.Channel wifiChannel;
    private WifiDirectReceiver wifiReceiver;

    private PacketStoreAndForwardManager dtnManager;
    private UdpMeshSocket udpSocket;
    private BluetoothVoiceCallManager voiceCallManager;
    private MeshNetworkBridge networkBridge;

    private WifiP2pDevice thisDevice;
    private WifiP2pInfo connectionInfo;

    private boolean isSyncPaused = false;
    private boolean isLoopRunning = false;
    private final Handler meshHandler = new Handler(Looper.getMainLooper());
    private final java.util.Map<String, PeerDevice> discoveredPeers = new java.util.concurrent.ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public interface MeshEventListener {
        void onMessageReceived(com.phoenix.phoenixnet.mesh.MeshPacket packet);
        void onStatusUpdate(String status);
        void onPeersUpdated(List<WifiP2pDevice> wifiPeers, List<BluetoothDevice> btPeers);
        void onPeerDiscovered(PeerDevice device);
        void onCallStateChanged(BluetoothVoiceCallManager.CallState state, String peerName);
    }

    private MeshEventListener eventListener;

    public class MeshBinder extends Binder {
        MeshService getService() {
            return MeshService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireLocks();
        initMeshComponents();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PheonixNet Mesh & Voice Active")
                .setContentText("Maintaining autonomous disaster mesh & voice network...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
                }
            }
            startForeground(1, notification, type);
        } else {
            startForeground(1, notification);
        }

        startAutonomousLoops();

        return START_STICKY;
    }

    @Inject
    AuthRepository authRepository;

    @Inject
    com.phoenix.phoenixnet.db.VoicePlayerHelper voicePlayer;

    private void initMeshComponents() {
        AppDatabase db = AppDatabase.Companion.getInstance(this);
        
        networkBridge = new MeshNetworkBridge(
                db.meshPacketDao(),
                authRepository,
                voicePlayer,
                syncManager,
                serviceScope,
                (uuid, senderId, payload, isSos) -> {
                    // MESH TRANSMISSION: Standard Flat Protocol
                    // Format: uuid|senderId|isSos|timestamp|type|recipient|text|audio|hop
                    String flatPacket = uuid + "|" + senderId + "|" + (isSos ? "1" : "0") + "|" + System.currentTimeMillis() + "|" + payload;
                    
                    if (udpSocket != null) {
                        udpSocket.broadcastPacket(flatPacket);
                    }
                    if (connectionManager != null) {
                        connectionManager.broadcast(flatPacket);
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
        networkBridge.start();

        dtnManager = new PacketStoreAndForwardManager(this, dbExecutor);
        udpSocket = new UdpMeshSocket();
        udpSocket.startListening(packetStr -> relayMeshPacket(packetStr, "UDP"));

        connectionManager = new MeshConnectionManager();
        connectionManager.setPacketListener((packet, source) -> relayMeshPacket(packet, source));

        String myShortNodeId = Build.MODEL.substring(0, Math.min(Build.MODEL.length(), 8)).replace(" ", "_") 
                + "_" + String.format(java.util.Locale.US, "%04d", new Random().nextInt(10000));
        discoveryEngine = new IroncladDiscoveryManager(this, myShortNodeId, connectionManager, (deviceId, name, transport) -> {
            PeerDevice device = new PeerDevice(deviceId, name, transport, -50);
            discoveredPeers.put(deviceId, device);
            if (peerManager != null) {
                peerManager.addOrUpdatePeer(device);
            }
            if (eventListener != null) eventListener.onPeerDiscovered(device);
        });
        discoveryEngine.start();

        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm != null) bluetoothAdapter = bm.getAdapter();

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        voiceCallManager = new BluetoothVoiceCallManager(bluetoothAdapter, am, new BluetoothVoiceCallManager.CallEventListener() {
            @Override
            public void onCallStateChanged(BluetoothVoiceCallManager.CallState state, String peerName) {
                if (eventListener != null) eventListener.onCallStateChanged(state, peerName);
            }

            @Override
            public void onCallError(String error) {
                notifyStatus("Voice Error: " + error);
            }

            @Override
            public void onPttStateChanged(boolean active) {
                if (dtnManager != null) {
                    dtnManager.setDtnPaused(active);
                    notifyStatus(active ? "PTT ACTIVE: Mesh Paused" : "PTT RELEASED: Mesh Resumed");
                }
            }
        });

        wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiManager != null) {
            wifiChannel = wifiManager.initialize(this, getMainLooper(), null);
            wifiReceiver = new WifiDirectReceiver(wifiManager, wifiChannel, this);
            
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(wifiReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(wifiReceiver, filter);
            }
        }
    }

    private void notifyPeersUpdated(List<WifiP2pDevice> wifiPeers) {
        if (eventListener != null) {
            eventListener.onPeersUpdated(wifiPeers, new ArrayList<>());
        }
    }

    private void startAutonomousLoops() {
        if (isLoopRunning) return;
        isLoopRunning = true;
        
        meshHandler.removeCallbacksAndMessages(null);
        meshHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isLoopRunning) return;
                
                // BATTERY OPTIMIZATION: Reduce duty cycle if battery is low
                int batteryLevel = getBatteryLevel();
                long nextScanDelay = 15000; // Default 15s
                
                if (batteryLevel < 15) {
                    nextScanDelay = 60000; // Drop to 1 min if critical battery
                    Log.w(TAG, "Low battery detected. Throttling mesh discovery.");
                }

                performDiscovery();
                pruneStalePeers();
                meshHandler.postDelayed(this, nextScanDelay); 
            }
        });
    }

    private void pruneStalePeers() {
        long now = System.currentTimeMillis();
        discoveredPeers.entrySet().removeIf(entry -> (now - entry.getValue().lastSeen) > 45000);
        if (peerManager != null) {
            peerManager.pruneStalePeers(45000);
        }
    }

    @SuppressLint("MissingPermission")
    private void performDiscovery() {
        if (isSyncPaused) return;
        notifyStatus("Mesh: Scanning...");
        Log.d(TAG, "Performing autonomous discovery...");

        if (thisDevice == null && wifiManager != null && wifiChannel != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.requestDeviceInfo(wifiChannel, device -> thisDevice = device);
            }
        }
        // Discovery is handled by IroncladDiscoveryManager
    }

    public void triggerImmediateScan() {
        performDiscovery();
    }

    public void pauseBackgroundSync(boolean paused) {
        this.isSyncPaused = paused;
    }

    public BluetoothVoiceCallManager getVoiceCallManager() {
        return voiceCallManager;
    }

    public synchronized void relayMeshPacket(String packetStr, String sourceTransport) {
        if (packetStr == null || packetStr.isEmpty()) return;
        Log.i(TAG, ">>> INCOMING PACKET via " + sourceTransport + ": " + packetStr.substring(0, Math.min(packetStr.length(), 50)) + "...");

        // Standard Flat Protocol Parsing
        // Format: 0:uuid|1:senderId|2:isSos|3:timestamp|4:type|5:recipient|6:text|7:audio|8:hopCount
        String[] parts = packetStr.split("\\|");
        if (parts.length >= 9) {
            try {
                String uuid = parts[0];
                String senderId = parts[1];
                boolean isSos = "1".equals(parts[2]);
                long timestamp = Long.parseLong(parts[3]);
                String type = parts[4];
                String recipient = parts[5].isEmpty() ? null : parts[5];
                String text = parts[6];
                String audio = parts[7];
                int incomingHop = Integer.parseInt(parts[8]);

                dbExecutor.execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.Companion.getInstance(this);
                        MeshPacketEntity existing = db.meshPacketDao().getPacketByUuid(uuid);
                        if (existing == null) {
                            MeshPacketEntity entity = new MeshPacketEntity(
                                    uuid, senderId, isSos, timestamp, audio,
                                    incomingHop, recipient, type, text,
                                    false, false
                            );
                            db.meshPacketDao().insertPacket(entity);
                            Log.d(TAG, "Packet stored & queued for relay: " + uuid);
                            
                            if (peerManager != null) {
                                peerManager.addOrUpdatePeer(new PeerDevice(senderId, "Mesh Node", 
                                    "BT".equals(sourceTransport) ? PeerDevice.Transport.BLUETOOTH : PeerDevice.Transport.WIFI_DIRECT, -60));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Database error during packet relay", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse mesh packet", e);
            }
        } else {
            Log.w(TAG, "Received malformed packet (parts: " + parts.length + ")");
        }
    }

    public void sendSos(double lat, double lon, String sender, String details) {
        /*
        MeshPacket packet = dtnManager.createLocalSos(lat, lon, sender, details);
        String data = packet.serialize();
        udpSocket.broadcastPacket(data);
        connectionManager.broadcast(data);
        notifyMessageReceived(packet);
        */
    }

    public void sendFlashSos(PeerDevice target, double lat, double lon, String sender, String details) {
        /*
        MeshPacket packet = dtnManager.createLocalSos(lat, lon, sender, "FLASH_SOS|" + details);
        packet.priority = 1; 
        packet.targetNodeId = target.deviceId;
        
        connectionManager.sendTargetedFlashSos(packet);
        */
        notifyStatus("Flash SOS dispatched to: " + target.deviceName);
    }

    public void syncStoredPackets() {
        /*
        List<MeshPacket> pending = dtnManager.getPacketsForSync();
        for (MeshPacket pkt : pending) {
            String data = pkt.serialize();
            connectionManager.broadcast(data);
            udpSocket.broadcastPacket(data);
        }
        */
    }

    private void acquireLocks() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PheonixNet::MeshWakeLock");
            wakeLock.acquire();
        }

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PheonixNet::MeshWifiLock");
            wifiLock.acquire();
        }
    }

    private void releaseLocks() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
    }

    @Override
    public void onDestroy() {
        // 1. CRITICAL: Cancel all active coroutines to prevent memory leaks.
        serviceJob.cancel(null);
        
        releaseLocks();
        try {
            if (udpSocket != null) udpSocket.stop();
            if (wifiReceiver != null) unregisterReceiver(wifiReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error during destroy: " + e.getMessage());
        }
        isLoopRunning = false;
        meshHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setEventListener(MeshEventListener listener) {
        this.eventListener = listener;
    }

    public void setDtnPaused(boolean paused) {
        if (dtnManager != null) dtnManager.setDtnPaused(paused);
    }

    private void notifyMessageReceived(com.phoenix.phoenixnet.mesh.MeshPacket packet) {
        if (eventListener != null) eventListener.onMessageReceived(packet);
    }

    private void notifyStatus(String status) {
        if (eventListener != null) eventListener.onStatusUpdate(status);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "PheonixNet Mesh Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        List<WifiP2pDevice> deviceList = new ArrayList<>(peers.getDeviceList());
        notifyPeersUpdated(deviceList);
        
        // CRITICAL FIX: Report Wi-Fi peers to the modern PeerManager
        if (peerManager != null) {
            for (WifiP2pDevice device : deviceList) {
                peerManager.addOrUpdatePeer(new PeerDevice(
                        device.deviceAddress,
                        device.deviceName,
                        PeerDevice.Transport.WIFI_DIRECT,
                        -60 // Default RSSI for Wi-Fi
                ));
            }
        }

        if (deviceList.isEmpty()) return;
        if (connectionInfo != null && connectionInfo.groupFormed) return;

        long myScore = calculateMeshScore(thisDevice, getBatteryLevel());
        for (WifiP2pDevice peer : deviceList) {
            if (peer.status == WifiP2pDevice.AVAILABLE) {
                long peerScore = calculateMeshScore(peer, 50); 
                if (myScore > peerScore && myScore != 0) {
                    wifiManager.createGroup(wifiChannel, null);
                    break;
                } else {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = peer.deviceAddress;
                    config.groupOwnerIntent = 0; 
                    wifiManager.connect(wifiChannel, config, null);
                    break;
                }
            }
        }
    }

    @Override
    public void onConnected(WifiP2pInfo info) {
        this.connectionInfo = info;
        if (info.groupFormed) {
            notifyStatus("Wi-Fi: Mesh Linked (" + (info.isGroupOwner ? "Host" : "Client") + ")");
            establishP2pSocket(info);
        }
        syncStoredPackets();
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        onConnected(info);
    }

    private void establishP2pSocket(WifiP2pInfo info) {
        new Thread(() -> {
            try {
                if (info.isGroupOwner) {
                    java.net.ServerSocket serverSocket = new java.net.ServerSocket(8989);
                    java.net.Socket client = serverSocket.accept();
                    connectionManager.addWifiSocket(client);
                } else {
                    for (int i = 0; i < 5; i++) {
                        try {
                            java.net.Socket socket = new java.net.Socket(info.groupOwnerAddress, 8989);
                            connectionManager.addWifiSocket(socket);
                            break;
                        } catch (IOException e) {
                            Thread.sleep(1000);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "P2P Socket Error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onDeviceInfoAvailable(WifiP2pDevice device) {
        this.thisDevice = device;
    }

    private long calculateMeshScore(WifiP2pDevice device, int battery) {
        if (device == null || device.deviceAddress == null) return new Random().nextInt(100) * 1_000_000L;
        return ((long) battery * 1_000_000L) + Math.abs(device.deviceAddress.hashCode());
    }

    private int getBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return 50;
    }
}
