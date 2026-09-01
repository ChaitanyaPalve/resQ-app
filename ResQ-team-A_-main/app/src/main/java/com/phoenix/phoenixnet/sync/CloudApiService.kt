package com.phoenix.phoenixnet.sync

import com.phoenix.phoenixnet.db.MeshPacketEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Modern Retrofit interface for the PhoenixNet FastAPI cloud backend.
 * Hardened for unreliable network conditions.
 */
interface CloudApiService {

    /**
     * Uploads a batch of mesh packets to the central database.
     * Endpoint: /api/v1/mesh/sync
     */
    @POST("/api/v1/mesh/sync")
    suspend fun syncPackets(@Body packets: List<MeshPacketEntity>): Response<Unit>
}
