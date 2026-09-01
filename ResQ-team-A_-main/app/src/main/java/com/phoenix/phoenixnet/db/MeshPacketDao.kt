package com.phoenix.phoenixnet.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshPacketDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPacket(packet: MeshPacketEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPackets(packets: List<MeshPacketEntity>): List<Long>

    @Update
    fun updatePacket(packet: MeshPacketEntity): Int

    @Query("DELETE FROM mesh_packets WHERE uuid = :uuid")
    fun deletePacketByUuid(uuid: String): Int

    @Query("SELECT * FROM mesh_packets WHERE uuid = :uuid LIMIT 1")
    fun getPacketByUuid(uuid: String): MeshPacketEntity?

    @Query("SELECT * FROM mesh_packets ORDER BY timestamp DESC")
    fun getAllPacketsFlow(): Flow<List<MeshPacketEntity>>

    @Query("SELECT * FROM mesh_packets WHERE is_broadcasted = 0")
    fun getUnbroadcastedPackets(): List<MeshPacketEntity>

    @Query("UPDATE mesh_packets SET is_broadcasted = 1 WHERE uuid = :uuid")
    fun markAsBroadcasted(uuid: String): Int

    @Query("SELECT * FROM mesh_packets WHERE is_synced_to_cloud = 0")
    fun getUnsyncedPackets(): List<MeshPacketEntity>

    @Query("UPDATE mesh_packets SET is_synced_to_cloud = 1 WHERE uuid = :uuid")
    fun markAsSynced(uuid: String): Int

    @Query("DELETE FROM mesh_packets WHERE timestamp < :expiryTimestamp")
    fun purgeExpiredPackets(expiryTimestamp: Long): Int
}
