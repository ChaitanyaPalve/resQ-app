package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothVoiceCallManager {
    private static final String TAG = "VoiceCallManager";
    private static final String APP_NAME = "PheonixNetVoice";
    private static final UUID VOICE_UUID = UUID.fromString("7b34f0e0-afac-11de-8a39-0800200c9a66");

    private final BluetoothAdapter bluetoothAdapter;
    private final AudioManager audioManager;
    private final LiveVoiceStreamManager streamManager;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private CallThread callThread;

    public enum CallState { IDLE, LISTENING, CONNECTING, CONNECTED }
    private CallState currentState = CallState.IDLE;
    private volatile boolean isPttActive = false;

    public interface CallEventListener {
        void onCallStateChanged(CallState state, String peerName);
        void onCallError(String error);
        void onPttStateChanged(boolean active);
    }

    private final CallEventListener eventListener;

    public BluetoothVoiceCallManager(BluetoothAdapter adapter, AudioManager audioManager, CallEventListener listener) {
        this.bluetoothAdapter = adapter;
        this.audioManager = audioManager;
        this.eventListener = listener;
        this.streamManager = new LiveVoiceStreamManager();
    }

    public synchronized void startListening() {
        if (bluetoothAdapter == null) return;
        stopAll();
        acceptThread = new AcceptThread();
        acceptThread.start();
        updateState(CallState.LISTENING, null);
    }

    @SuppressLint("MissingPermission")
    public synchronized void startCall(BluetoothDevice device) {
        if (bluetoothAdapter == null || device == null) return;
        stopAll();
        connectThread = new ConnectThread(device);
        connectThread.start();
        updateState(CallState.CONNECTING, device.getName());
    }

    public synchronized void endCall() {
        stopAll();
        updateState(CallState.IDLE, null);
    }

    public void setPttActive(boolean active) {
        this.isPttActive = active;
        streamManager.setPttActive(active);
        Log.d(TAG, "PTT Active: " + active);
        if (eventListener != null) eventListener.onPttStateChanged(active);
    }

    private synchronized void stopAll() {
        if (acceptThread != null) { acceptThread.cancel(); acceptThread = null; }
        if (connectThread != null) { connectThread.cancel(); connectThread = null; }
        if (callThread != null) { callThread.cancel(); callThread = null; }
        streamManager.stopAll();
        resetAudioRouting();
    }

    private void updateState(CallState state, String peer) {
        this.currentState = state;
        if (eventListener != null) eventListener.onCallStateChanged(state, peer);
    }

    private AudioFocusRequest audioFocusRequest;

    private void configureAudioRouting() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    private void resetAudioRouting() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, VOICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen failed", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                if (serverSocket != null) socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket accept failed", e);
            }

            if (socket != null) {
                synchronized (BluetoothVoiceCallManager.this) {
                    startCallThread(socket);
                }
            }
        }

        public void cancel() {
            try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(VOICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create failed", e);
            }
            socket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                synchronized (BluetoothVoiceCallManager.this) {
                    startCallThread(socket);
                }
            } catch (IOException e) {
                try { socket.close(); } catch (IOException ignored) {}
                if (eventListener != null) eventListener.onCallError("Connect Failed: " + e.getMessage());
                updateState(CallState.IDLE, null);
            }
        }

        public void cancel() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    @SuppressLint("MissingPermission")
    private void startCallThread(BluetoothSocket socket) {
        if (callThread != null) callThread.cancel();
        callThread = new CallThread(socket);
        callThread.start();
        configureAudioRouting();
        updateState(CallState.CONNECTED, socket.getRemoteDevice().getName());
    }

    private class CallThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream in;
        private final OutputStream out;
        private boolean isRunning = true;

        public CallThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) {}
            in = tmpIn;
            out = tmpOut;
        }

        public void run() {
            streamManager.startStreamingAudio(out);
            streamManager.listenForIncomingAudio(in);
            
            while (isRunning && socket.isConnected()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            isRunning = false;
            streamManager.stopAll();
        }

        public void cancel() {
            isRunning = false;
            streamManager.stopAll();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
