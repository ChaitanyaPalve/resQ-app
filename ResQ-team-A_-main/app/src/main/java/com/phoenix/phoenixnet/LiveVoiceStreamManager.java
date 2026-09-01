package com.phoenix.phoenixnet;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Principal Android Audio & Networking Engineer:
 * Dedicated manager for real-time PCM voice streaming over network sockets.
 */
public class LiveVoiceStreamManager {
    private static final String TAG = "LiveVoiceStream";
    
    private static final int SAMPLE_RATE = 16000;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;

    private AudioRecord recorder;
    private AudioTrack player;
    
    private volatile boolean isStreaming = false;
    private volatile boolean isListening = false;
    private volatile boolean pttPressed = false;

    private AcousticEchoCanceler aec;
    private NoiseSuppressor ns;

    public LiveVoiceStreamManager() {}

    /**
     * Starts capturing microphone data and writing it to the provided OutputStream.
     * Runs on a dedicated background thread.
     */
    @SuppressLint("MissingPermission")
    public void startStreamingAudio(final OutputStream outStream) {
        if (isStreaming) return;
        isStreaming = true;

        new Thread(() -> {
            int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING);
            if (minBufSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid AudioRecord parameters.");
                return;
            }

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_IN, ENCODING, minBufSize * 2);
            
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.");
                return;
            }

            // Hardware-level Audio Enhancement
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(recorder.getAudioSessionId());
                if (aec != null) aec.setEnabled(true);
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(recorder.getAudioSessionId());
                if (ns != null) ns.setEnabled(true);
            }

            byte[] buffer = new byte[minBufSize];
            recorder.startRecording();
            Log.d(TAG, "Audio capture started.");

            try {
                while (isStreaming) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0 && pttPressed) {
                        outStream.write(buffer, 0, read);
                        outStream.flush();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Streaming error: " + e.getMessage());
            } finally {
                releaseRecorder();
            }
        }).start();
    }

    /**
     * Listens for incoming PCM bytes from the InputStream and plays them via AudioTrack.
     * Runs on a dedicated background thread.
     */
    public void listenForIncomingAudio(final InputStream inStream) {
        if (isListening) return;
        isListening = true;

        new Thread(() -> {
            int minBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING);
            
            player = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(ENCODING)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_OUT)
                            .build())
                    .setBufferSizeInBytes(minBufSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            if (player.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack initialization failed.");
                return;
            }

            player.play();
            Log.d(TAG, "Audio playback started.");

            byte[] buffer = new byte[minBufSize];
            try {
                while (isListening) {
                    int read = inStream.read(buffer);
                    if (read > 0) {
                        player.write(buffer, 0, read);
                    } else if (read == -1) {
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Playback error: " + e.getMessage());
            } finally {
                releasePlayer();
            }
        }).start();
    }

    public void setPttActive(boolean active) {
        this.pttPressed = active;
        Log.d(TAG, "PTT: " + (active ? "PRESSED" : "RELEASED"));
    }

    public void stopAll() {
        isStreaming = false;
        isListening = false;
        pttPressed = false;
    }

    private synchronized void releaseRecorder() {
        if (recorder != null) {
            try {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop();
                }
            } catch (Exception ignored) {}
            recorder.release();
            recorder = null;
        }
        if (aec != null) { aec.release(); aec = null; }
        if (ns != null) { ns.release(); ns = null; }
        Log.d(TAG, "Recorder released.");
    }

    private synchronized void releasePlayer() {
        if (player != null) {
            try {
                if (player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    player.stop();
                }
            } catch (Exception ignored) {}
            player.release();
            player = null;
        }
        Log.d(TAG, "Player released.");
    }
}
