package com.phoenix.phoenixnet.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
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
import com.phoenix.phoenixnet.db.MeshPacketEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshInboxScreen(
    viewModel: MeshInboxViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Inbox", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No mesh messages received.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageItem(message, onPlayVoice = { viewModel.playVoiceMessage(it) })
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: MeshPacketEntity, onPlayVoice: (String) -> Unit) {
    val isSos = message.isSos || message.messageType == "SOS"
    val backgroundColor = if (isSos) Color(0xFF440000) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSos) Color.Red else Color.Transparent

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSos) "⚠️ SOS BROADCAST" else if (message.recipientId == null) "📡 MESH BROADCAST" else "🎯 DIRECT MESSAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSos) Color.Red else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "From: ${message.senderId.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(4.dp))

            when (message.messageType) {
                "TEXT" -> {
                    Text(
                        text = message.textContent ?: "[Empty Text]",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                "VOICE", "SOS" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPlayVoice(message.payloadBase64) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Voice")
                        }
                        Text("Voice Note (${message.payloadBase64.length / 1024} KB)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
