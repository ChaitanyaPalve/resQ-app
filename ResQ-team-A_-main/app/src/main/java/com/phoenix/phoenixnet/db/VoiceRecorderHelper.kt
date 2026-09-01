package com.phoenix.phoenixnet.db

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Robust helper for managing Android's MediaRecorder state machine.
 * Optimized for high-compression voice data (AMR-WB) in DTN environments.
 */
class VoiceRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    companion object {
        private const val TAG = "VoiceRecorderHelper"
        private const val TEMP_FILE_PREFIX = "blockvoice_"
        private const val TEMP_FILE_SUFFIX = ".amr"
    }

    /**
     * Starts a new recording session.
     * @throws IOException if the file or recorder cannot be initialized.
     */
    fun startRecording() {
        if (mediaRecorder != null) {
            Log.w(TAG, "Recording already in progress. Resetting.")
            stopAndCancel()
        }

        try {
            currentFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, context.cacheDir)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                setOutputFile(currentFile?.absolutePath)
                
                prepare()
                start()
            }
            Log.d(TAG, "Recording started: ${currentFile?.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            stopAndCancel()
            throw e
        }
    }

    /**
     * Stops the recording and returns the Base64 encoded audio string.
     * Guaranteed to release resources and delete the temporary file.
     */
    fun stopAndGetBase64(): String? {
        val recorder = mediaRecorder ?: return null
        var encodedResult: String? = null

        try {
            try {
                recorder.stop()
            } catch (e: RuntimeException) {
                // Occurs if stop() is called immediately after start() before any data is recorded.
                Log.w(TAG, "Stop failed (recording too short?)", e)
            }
            
            recorder.release()
            mediaRecorder = null

            val file = currentFile
            if (file != null && file.exists()) {
                val bytes = file.readBytes()
                if (bytes.isNotEmpty()) {
                    encodedResult = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    Log.d(TAG, "Recording encoded to Base64 (Size: ${bytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing recording", e)
        } finally {
            cleanup()
        }

        return encodedResult
    }

    /**
     * Aborts the current recording and releases resources without returning data.
     */
    fun stopAndCancel() {
        try {
            mediaRecorder?.stop()
        } catch (ignored: Exception) {
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
            cleanup()
        }
    }

    private fun cleanup() {
        mediaRecorder = null
        try {
            currentFile?.let {
                if (it.exists()) {
                    it.delete()
                    Log.d(TAG, "Temp file deleted: ${it.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete temp file", e)
        }
        currentFile = null
    }
}
