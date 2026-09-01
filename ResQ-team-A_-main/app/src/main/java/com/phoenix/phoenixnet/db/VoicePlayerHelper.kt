package com.phoenix.phoenixnet.db

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper for decoding and playing AMR-WB mesh voice packets.
 */
class VoicePlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "VoicePlayerHelper"
    }

    /**
     * Decodes a Base64 AMR string and plays it immediately.
     */
    fun playBase64Audio(base64Data: String) {
        if (base64Data.isEmpty()) return

        stopPlayback()

        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val tempFile = File.createTempFile("mesh_play_", ".amr", context.cacheDir)
            
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio packet", e)
        }
    }

    fun stopPlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}
