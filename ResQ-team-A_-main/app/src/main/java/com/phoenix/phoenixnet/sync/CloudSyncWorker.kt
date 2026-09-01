package com.phoenix.phoenixnet.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoenix.phoenixnet.db.MeshPacketDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hardened WorkManager background worker for Cloud Synchronization.
 * Implements exponential backoff and connection-aware execution.
 */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val meshPacketDao: MeshPacketDao,
    private val cloudApi: CloudApiService
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CloudSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initiating cloud synchronization pulse...")

        try {
            val unsyncedPackets = meshPacketDao.getUnsyncedPackets()
            if (unsyncedPackets.isEmpty()) {
                Log.d(TAG, "No unsynced packets found. Synchronization complete.")
                return@withContext Result.success()
            }

            Log.d(TAG, "Attempting to sync ${unsyncedPackets.size} packets to cloud.")
            
            val response = cloudApi.syncPackets(unsyncedPackets)

            if (response.isSuccessful) {
                Log.i(TAG, "Batch synchronization successful. Marking packets as synced.")
                unsyncedPackets.forEach { packet ->
                    meshPacketDao.markAsSynced(packet.uuid)
                }
                Result.success()
            } else {
                Log.w(TAG, "Cloud API failed with code: ${response.code()}. Retrying with backoff.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during cloud synchronization", e)
            Result.retry()
        }
    }
}
