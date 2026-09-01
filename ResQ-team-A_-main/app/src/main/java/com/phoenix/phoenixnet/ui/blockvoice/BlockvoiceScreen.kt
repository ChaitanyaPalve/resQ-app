package com.phoenix.phoenixnet.ui.blockvoice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockvoiceScreen(viewModel: BlockvoiceViewModel) {
    val recordingState by viewModel.recordingState.collectAsState()
    val isSosActive by viewModel.isSosActive.collectAsState()
    val panicCountdown by viewModel.panicCountdown.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onHoldToTalkStart()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Microphone permission is required for emergency voice mesh.",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Blockvoice Mesh") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SOS", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isSosActive,
                            onCheckedChange = { viewModel.toggleSos(it) }
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Panic Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = { viewModel.startPanicBroadcast() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PANIC BROADCAST", fontWeight = FontWeight.Bold)
                }
                
                panicCountdown?.let {
                    Text(
                        "Automatic recording in: $it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Central Status Display
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Text(
                    text = when (recordingState) {
                        RecordingState.RECORDING -> "RECORDING..."
                        RecordingState.LOCKED -> "REVIEW MODE"
                        RecordingState.CANCELLED -> "CANCELLED"
                        else -> "Hold button to speak"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (recordingState == RecordingState.RECORDING) Color.Red else Color.Unspecified
                )
            }

            // Interaction Area
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (recordingState == RecordingState.LOCKED) {
                    // Playback Review Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* Play logic */ }) {
                                Icon(Icons.Default.PlayArrow, "Play")
                            }
                            IconButton(onClick = { viewModel.discardRecording() }) {
                                Icon(Icons.Default.Delete, "Trash", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { viewModel.onHoldToTalkRelease(false) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    HoldToTalkButton(
                        viewModel = viewModel,
                        onPermissionRequired = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HoldToTalkButton(
    viewModel: BlockvoiceViewModel,
    onPermissionRequired: () -> Unit
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsState()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Visual feedback for recording state
    val buttonScale by animateFloatAsState(if (recordingState == RecordingState.RECORDING) 1.2f else 1f, label = "button_scale")

    Box(
        modifier = Modifier
            .size(120.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .scale(buttonScale)
            .clip(CircleShape)
            .background(if (recordingState == RecordingState.RECORDING) Color.Red else MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.onHoldToTalkStart()
                        } else {
                            onPermissionRequired()
                        }
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        // CONSUME pointers to prevent UI scrolling
                        change.consume()
                        
                        if (viewModel.recordingState.value == RecordingState.RECORDING) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            
                            // SWIPE LEFT THRESHOLD (-50f)
                            if (offsetX < -150f) { // Approximately -50dp depending on density, adjusted for sensitivity
                                viewModel.onHoldToTalkCancel()
                            }
                            
                            // SWIPE UP TO LOCK (Threshold e.g. -150f)
                            if (offsetY < -150f) {
                                viewModel.lockRecording()
                            }
                        }
                    },
                    onDragEnd = {
                        if (viewModel.recordingState.value == RecordingState.RECORDING) {
                            viewModel.onHoldToTalkRelease(false)
                        }
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = {
                        viewModel.onHoldToTalkCancel()
                        offsetX = 0f
                        offsetY = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (recordingState == RecordingState.RECORDING) "RECORD" else "PTT",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}
