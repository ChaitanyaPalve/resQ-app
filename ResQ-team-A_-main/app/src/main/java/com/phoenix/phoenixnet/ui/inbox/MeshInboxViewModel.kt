package com.phoenix.phoenixnet.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.phoenixnet.db.AuthRepository
import com.phoenix.phoenixnet.db.MeshPacketDao
import com.phoenix.phoenixnet.db.MeshPacketEntity
import com.phoenix.phoenixnet.db.VoicePlayerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MeshInboxViewModel @Inject constructor(
    private val meshPacketDao: MeshPacketDao,
    private val authRepository: AuthRepository,
    private val voicePlayer: VoicePlayerHelper
) : ViewModel() {

    val messages: StateFlow<List<MeshPacketEntity>> = meshPacketDao.getAllPacketsFlow()
        .map { packets ->
            val myId = authRepository.getActiveSession()?.userId
            packets.filter { it.recipientId == null || it.recipientId == myId }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun playVoiceMessage(base64: String) {
        voicePlayer.playBase64Audio(base64)
    }
}
