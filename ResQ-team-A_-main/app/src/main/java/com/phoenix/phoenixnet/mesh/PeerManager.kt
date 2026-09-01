package com.phoenix.phoenixnet.mesh

import com.phoenix.phoenixnet.PeerDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe manager for discovered mesh nodes.
 * Bridges the legacy MeshService discovery with modern Compose UI.
 */
@Singleton
class PeerManager @Inject constructor() {

    private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val peers: StateFlow<List<PeerDevice>> = _peers.asStateFlow()

    /**
     * Updates or adds a peer to the active list.
     */
    fun addOrUpdatePeer(device: PeerDevice) {
        val currentList = _peers.value.toMutableList()
        val index = currentList.indexOfFirst { it.deviceId == device.deviceId }
        
        if (index != -1) {
            currentList[index] = device
        } else {
            currentList.add(device)
        }
        _peers.value = currentList
    }

    /**
     * Removes peers that haven't been seen for a specified duration.
     */
    fun pruneStalePeers(timeoutMs: Long = 45000) {
        val now = System.currentTimeMillis()
        val filtered = _peers.value.filter { (now - it.lastSeen) <= timeoutMs }
        if (filtered.size != _peers.value.size) {
            _peers.value = filtered
        }
    }

    fun updatePeerLocation(deviceId: String, lat: Double, lon: Double) {
        val currentList = _peers.value.toMutableList()
        val index = currentList.indexOfFirst { it.deviceId == deviceId }
        if (index != -1) {
            val peer = currentList[index]
            peer.updateLocation(lat, lon)
            currentList[index] = peer
            _peers.value = currentList
        }
    }

    /**
     * Clears all discovered peers (e.g., on service reset).
     */
    fun clearPeers() {
        _peers.value = emptyList()
    }
}
