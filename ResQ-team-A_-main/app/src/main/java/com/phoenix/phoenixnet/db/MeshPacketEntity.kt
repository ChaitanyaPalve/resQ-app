package com.phoenix.phoenixnet.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Modern Mesh Packet Entity for PhoenixNet DTN.
 * 
 * SECURITY NOTE: payloadBase64 may contain sensitive AMR-WB data.
 * Inject SQLCipher in AppDatabase to ensure encryption-at-rest.
 */
@Entity(
    tableName = "mesh_packets",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["is_synced_to_cloud"])
    ]
)
data class MeshPacketEntity @JvmOverloads constructor(
    @PrimaryKey
    val uuid: String,
    
    @ColumnInfo(name = "sender_id")
    val senderId: String,
    
    @ColumnInfo(name = "is_sos", defaultValue = "0")
    val isSos: Boolean = false,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * Base64 encoded payload, potentially containing large AMR-WB audio data.
     */
    @ColumnInfo(name = "payload_base64")
    val payloadBase64: String,
    
    @ColumnInfo(name = "hop_count", defaultValue = "0")
    val hopCount: Int = 0,
    
    @ColumnInfo(name = "recipient_id")
    val recipientId: String? = null,
    
    @ColumnInfo(name = "message_type")
    val messageType: String = "VOICE", // VOICE, TEXT, SOS
    
    @ColumnInfo(name = "text_content")
    val textContent: String? = null,
    
    @ColumnInfo(name = "is_broadcasted", defaultValue = "0")
    val isBroadcasted: Boolean = false,
    
    @ColumnInfo(name = "is_synced_to_cloud", defaultValue = "0")
    val isSyncedToCloud: Boolean = false
)
