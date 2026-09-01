package com.phoenix.phoenixnet.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.phoenixnet.PeerDevice
import com.phoenix.phoenixnet.db.AuthRepository
import com.phoenix.phoenixnet.db.MeshPacketDao
import com.phoenix.phoenixnet.db.MeshPacketEntity
import com.phoenix.phoenixnet.mesh.PeerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class MeshStats(
    val pendingPackets: Int,
    val syncedPackets: Int,
    val totalNodes: Int
)

@HiltViewModel
class MeshDashboardViewModel @Inject constructor(
    private val peerManager: PeerManager,
    private val meshPacketDao: MeshPacketDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    val peers: StateFlow<List<PeerDevice>> = peerManager.peers

    private val _currentGeoPoint = MutableStateFlow(GeoPoint(18.5204, 73.8567))
    val currentGeoPoint: StateFlow<GeoPoint> = _currentGeoPoint.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    val meshStats: StateFlow<MeshStats> = meshPacketDao.getAllPacketsFlow()
        .map { packets ->
            MeshStats(
                pendingPackets = packets.count { !it.isSyncedToCloud },
                syncedPackets = packets.count { it.isSyncedToCloud },
                totalNodes = peers.value.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MeshStats(0, 0, 0)
        )

    private val _isFireMode = MutableStateFlow(true) // Default to Fire
    val isFireMode: StateFlow<Boolean> = _isFireMode.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _currentLocation = MutableStateFlow("18.5204, 73.8567")
    val currentLocation: StateFlow<String> = _currentLocation.asStateFlow()

    fun toggleFireMode(enabled: Boolean) {
        _isFireMode.value = enabled
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
    }

    fun updateLocation(lat: Double, lon: Double) {
        val locStr = "%.4f, %.4f".format(java.util.Locale.US, lat, lon)
        _currentLocation.value = locStr
        _currentGeoPoint.value = GeoPoint(lat, lon)
    }

    fun sendSosBroadcast() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = authRepository.getActiveSession()
            val packet = MeshPacketEntity(
                uuid = java.util.UUID.randomUUID().toString(),
                senderId = user?.userId ?: "ANONYMOUS",
                recipientId = null,
                messageType = "SOS",
                isSos = true,
                timestamp = System.currentTimeMillis(),
                payloadBase64 = android.util.Base64.encodeToString("GENERAL_SOS".toByteArray(), android.util.Base64.NO_WRAP),
                hopCount = 0,
                isSyncedToCloud = false
            )
            meshPacketDao.insertPacket(packet)
            _navigationEvent.emit("inbox")
        }
    }

    fun sendCategorizedAlert(category: String) {
        val coords = _currentLocation.value
        viewModelScope.launch(Dispatchers.IO) {
            val user = authRepository.getActiveSession()
            val packet = MeshPacketEntity(
                uuid = java.util.UUID.randomUUID().toString(),
                senderId = user?.userId ?: "ANONYMOUS",
                recipientId = null,
                messageType = "TEXT",
                textContent = "URGENT: $category assistance required at $coords",
                isSos = true,
                timestamp = System.currentTimeMillis(),
                payloadBase64 = "",
                hopCount = 0,
                isSyncedToCloud = false
            )
            meshPacketDao.insertPacket(packet)
            _navigationEvent.emit("inbox")
        }
    }
}
