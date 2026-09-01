package com.phoenix.phoenixnet.db

import android.util.Log
import com.phoenix.phoenixnet.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Intermediary between Room Database and the Networking Layer.
 * 
 * DESIGN GOALS:
 * 1. Low Power: Observes Flow instead of polling.
 * 2. Stability: Uses Mutex and conflate() to handle backpressure and prevent broadcast storms.
 * 3. Scalability: Offloads all operations to Dispatchers.IO.
 */
class MeshNetworkBridge(
    private val meshPacketDao: MeshPacketDao,
    private val authRepository: AuthRepository,
    private val voicePlayer: VoicePlayerHelper,
    private val syncManager: SyncManager,
    private val coroutineScope: CoroutineScope,
    private val onTransmit: (packetId: String, senderId: String, payload: String, isSos: Boolean) -> Unit
) {
    private val mutex = Mutex()

    companion object {
        private const val TAG = "MeshNetworkBridge"
        private const val MAX_HOPS = 10
    }

    /**
     * Starts observing the database for unbroadcasted packets.
     */
    fun start() {
        coroutineScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Mesh Bridge started. Observing for unbroadcasted packets...")
            
            meshPacketDao.getAllPacketsFlow()
                .conflate()
                .collect { packets ->
                    mutex.withLock {
                        val unbroadcasted = packets.filter { !it.isBroadcasted }
                        if (unbroadcasted.isNotEmpty()) {
                            val myUserId = authRepository.getActiveSession()?.userId ?: "ANONYMOUS"
                            
                            unbroadcasted.forEach { entity ->
                                // ROUTING LOGIC:
                                // 1. If it's for ME, don't re-broadcast. (It's already saved in DB for UI)
                                // 2. If it's for someone else (or broadcast null), broadcast it.
                                
                                if (entity.recipientId != null && entity.recipientId == myUserId) {
                                    Log.i(TAG, "Packet [${entity.uuid}] is for ME. Consuming locally.")
                                    
                                    // Auto-play SOS/Direct voice if from someone else
                                    if (entity.senderId != myUserId && (entity.isSos || entity.messageType == "SOS" || entity.messageType == "VOICE")) {
                                        voicePlayer.playBase64Audio(entity.payloadBase64)
                                    }
                                    
                                    meshPacketDao.markAsBroadcasted(entity.uuid)
                                } else if (entity.hopCount < MAX_HOPS) {
                                    // Auto-play SOS broadcasts even if we are just a relay
                                    if (entity.senderId != myUserId && (entity.isSos || entity.messageType == "SOS")) {
                                        voicePlayer.playBase64Audio(entity.payloadBase64)
                                    }
                                    processPacket(entity)
                                } else {
                                    Log.w(TAG, "Packet [${entity.uuid}] exceeded MAX_HOPS. Dropping.")
                                    meshPacketDao.markAsBroadcasted(entity.uuid)
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun processPacket(entity: MeshPacketEntity) {
        try {
            val nextHop = entity.hopCount + 1
            Log.d(TAG, "Broadcasting packet [${entity.uuid}]. Hop: $nextHop, Recipient: ${entity.recipientId ?: "ALL"}")
            
            // Construct payload for legacy pipe format
            // Format: type|recipient|text|audioBase64|hopCount
            val compositePayload = "${entity.messageType}|${entity.recipientId ?: ""}|${entity.textContent ?: ""}|${entity.payloadBase64}|$nextHop"
            
            onTransmit(entity.uuid, entity.senderId, compositePayload, entity.isSos)
            
            val rows = meshPacketDao.markAsBroadcasted(entity.uuid)
            if (rows > 0) {
                syncManager.triggerImmediateSync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing packet [${entity.uuid}]", e)
        }
    }
}
