package com.phoenix.phoenixnet.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoenix.phoenixnet.PeerDevice
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDashboardScreen(viewModel: MeshDashboardViewModel) {
    val peers by viewModel.peers.collectAsState()
    val stats by viewModel.meshStats.collectAsState()
    val isSimMode by viewModel.isSimulationMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Dashboard") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SIM MODE", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = isSimMode,
                            onCheckedChange = { viewModel.toggleSimulationMode(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Stats Section
            StatsCard(stats)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Active Peer Nodes (${peers.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (peers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Searching for mesh nodes...", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(peers) { peer ->
                        PeerCard(peer)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(stats: MeshStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem("Pending", stats.pendingPackets.toString(), Color.Red)
            StatItem("Synced", stats.syncedPackets.toString(), Color(0xFF4CAF50))
            StatItem("Nodes", stats.totalNodes.toString(), MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PeerCard(peer: PeerDevice) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (peer.transport == PeerDevice.Transport.WIFI_DIRECT) Icons.Default.Wifi else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = peer.deviceName, fontWeight = FontWeight.Bold)
                Text(text = "ID: ${peer.deviceId.take(8)}...", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Last seen: ${sdf.format(Date(peer.lastSeen))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${peer.rssi} dBm", fontWeight = FontWeight.Medium)
                SignalStrengthBar(peer.rssi)
            }
        }
    }
}

@Composable
fun SignalStrengthBar(rssi: Int) {
    // Simple visualization of RSSI
    val bars = when {
        rssi > -60 -> 4
        rssi > -70 -> 3
        rssi > -80 -> 2
        else -> 1
    }
    Row {
        repeat(4) { index ->
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height((8 + (index * 4)).dp)
                    .padding(horizontal = 1.dp),
                color = if (index < bars) MaterialTheme.colorScheme.primary else Color.LightGray
            ) {}
        }
    }
}
