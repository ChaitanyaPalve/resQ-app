package com.phoenix.phoenixnet.ui.blockvoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.phoenixnet.db.AuthRepository
import com.phoenix.phoenixnet.db.MeshPacketDao
import com.phoenix.phoenixnet.db.MeshPacketEntity
import com.phoenix.phoenixnet.db.VoiceRecorderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

enum class RecordingState {
    IDLE, RECORDING, LOCKED, CANCELLED
}

/**
 * ViewModel for the Blockvoice screen.
 * Bridges the hardware VoiceRecorderHelper and the Room database.
 */
@HiltViewModel
class BlockvoiceViewModel @Inject constructor(
    private val voiceRecorder: VoiceRecorderHelper,
    private val meshPacketDao: MeshPacketDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    private val _targetRecipientId = MutableStateFlow<String?>(null)
    val targetRecipientId: StateFlow<String?> = _targetRecipientId.asStateFlow()

    var textInput by mutableStateOf("")

    private val _panicCountdown = MutableStateFlow<Int?>(null)
    val panicCountdown: StateFlow<Int?> = _panicCountdown.asStateFlow()

    fun setTarget(first: String, middle: String, last: String) {
        _targetRecipientId.value = generateTargetId(first, middle, last)
    }

    fun clearTarget() {
        _targetRecipientId.value = null
    }

    private fun generateTargetId(first: String, middle: String, last: String): String {
        val input = "${first.lowercase()}.${middle.lowercase()}.${last.lowercase()}".trim()
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun sendTextMessage() {
        val content = textInput.trim()
        if (content.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val user = authRepository.getActiveSession()
            val entity = MeshPacketEntity(
                uuid = UUID.randomUUID().toString(),
                senderId = user?.userId ?: "ANONYMOUS",
                recipientId = _targetRecipientId.value,
                messageType = "TEXT",
                textContent = content,
                isSos = false,
                timestamp = System.currentTimeMillis(),
                payloadBase64 = "", // No audio for text
                hopCount = 0,
                isSyncedToCloud = false
            )
            meshPacketDao.insertPacket(entity)
            textInput = "" // Clear on Main if needed? No, keeping here for now.
        }
    }

    fun toggleSos(active: Boolean) {
        _isSosActive.value = active
    }

    /**
     * Triggered by ACTION_DOWN gesture.
     */
    fun onHoldToTalkStart() {
        if (_recordingState.value == RecordingState.IDLE) {
            _recordingState.value = RecordingState.RECORDING
            _isRecording.value = true
            try {
                voiceRecorder.startRecording()
            } catch (e: Exception) {
                _recordingState.value = RecordingState.IDLE
                _isRecording.value = false
            }
        }
    }

    /**
     * Triggered by ACTION_UP gesture.
     * @param isPanic if true, indicates an automated emergency recording.
     */
    fun onHoldToTalkRelease(isPanic: Boolean = false) {
        if (_recordingState.value == RecordingState.RECORDING || isPanic) {
            _isRecording.value = false
            _recordingState.value = RecordingState.IDLE
            
            viewModelScope.launch(Dispatchers.IO) {
                val base64Audio = voiceRecorder.stopAndGetBase64()
                if (base64Audio != null) {
                    persistPacket(base64Audio, isPanic || _isSosActive.value)
                }
            }
        }
    }

    /**
     * Triggered by SWIPE LEFT or explicit cancel.
     */
    fun onHoldToTalkCancel() {
        if (_recordingState.value == RecordingState.RECORDING) {
            _isRecording.value = false
            _recordingState.value = RecordingState.CANCELLED
            voiceRecorder.stopAndCancel()
            
            viewModelScope.launch {
                delay(500)
                _recordingState.value = RecordingState.IDLE
            }
        }
    }

    fun startPanicBroadcast() {
        viewModelScope.launch {
            for (i in 5 downTo 1) { 
                _panicCountdown.value = i
                delay(1000)
            }
            _panicCountdown.value = null
            
            // Start automatic 5-second burst
            onHoldToTalkStart()
            delay(5000)
            onHoldToTalkRelease(isPanic = true)
        }
    }

    private fun persistPacket(audioBase64: String, isSos: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = authRepository.getActiveSession()
            val entity = MeshPacketEntity(
                uuid = UUID.randomUUID().toString().substring(0, 8),
                senderId = user?.userId ?: "ANONYMOUS",
                recipientId = _targetRecipientId.value,
                messageType = "VOICE",
                isSos = isSos,
                timestamp = System.currentTimeMillis(),
                payloadBase64 = audioBase64,
                hopCount = 0,
                isSyncedToCloud = false
            )

            try {
                meshPacketDao.insertPacket(entity)
            } catch (e: Exception) {
                // Log or handle insertion error
            }
        }
    }

    fun discardRecording() {
        _recordingState.value = RecordingState.IDLE
        voiceRecorder.stopAndCancel()
    }

    fun lockRecording() {
        _recordingState.value = RecordingState.LOCKED
    }
}
