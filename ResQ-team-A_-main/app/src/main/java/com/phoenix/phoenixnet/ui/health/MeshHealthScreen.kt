package com.phoenix.phoenixnet.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoenix.phoenixnet.PeerDevice
import com.phoenix.phoenixnet.ui.dashboard.MeshDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshHealthScreen(
    viewModel: MeshDashboardViewModel,
    onBack: () -> Unit
) {
    val peers by viewModel.peers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Health", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (peers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Searching for nearby mesh nodes...", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(peers) { peer ->
                    PeerHealthItem(peer)
                }
            }
        }
    }
}

@Composable
fun PeerHealthItem(peer: PeerDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (peer.transport == PeerDevice.Transport.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(peer.deviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("ID: ${peer.deviceId.take(12)}", fontSize = 10.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {
                val rssiColor = when {
                    peer.rssi > -60 -> Color.Green
                    peer.rssi > -80 -> Color.Yellow
                    else -> Color.Red
                }
                Text("${peer.rssi} dBm", color = rssiColor, fontWeight = FontWeight.Bold)
                Text(
                    text = if (System.currentTimeMillis() - peer.lastSeen < 10000) "ACTIVE" else "STALE",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
