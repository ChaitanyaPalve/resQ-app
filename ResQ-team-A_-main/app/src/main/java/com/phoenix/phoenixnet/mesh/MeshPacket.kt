package com.phoenix.phoenixnet.mesh

/**
 * Transport data class for mesh transmission.
 * Represents the payload sent over the Delay-Tolerant Network.
 */
data class MeshPacket(
    val packetId: String,
    val senderUuid: String,
    val isSos: Boolean,
    val timestamp: Long,
    val audioPayload: String // Base64 encoded AMR-WB audio
)
