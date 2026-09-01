package com.phoenix.phoenixnet.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.phoenix.phoenixnet.ui.blockvoice.BlockvoiceViewModel
import com.phoenix.phoenixnet.ui.blockvoice.RecordingState
import com.phoenix.phoenixnet.ui.theme.LocalPhoenixThemeState
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.roundToInt

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer

@Composable
fun MainDashboardScreen(
    meshViewModel: MeshDashboardViewModel,
    voiceViewModel: BlockvoiceViewModel,
    onNavigateToInbox: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onLogout: () -> Unit
) {
    val themeState = LocalPhoenixThemeState.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val recordingState by voiceViewModel.recordingState.collectAsState()
    val isFireMode = themeState.isFireMode
    val locationStr by meshViewModel.currentLocation.collectAsState()
    val peers by meshViewModel.peers.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) voiceViewModel.onHoldToTalkStart()
    }

    val currentGeoPoint by meshViewModel.currentGeoPoint.collectAsState()
    var capturedMapView by remember { mutableStateOf<MapView?>(null) }

    // Tactical Redirects
    LaunchedEffect(meshViewModel.navigationEvent) {
        meshViewModel.navigationEvent.collect { event ->
            if (event == "inbox") onNavigateToInbox()
        }
    }

    // Lifecycle management for OSMDroid GPS
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> capturedMapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> capturedMapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. ALWAYS VISIBLE: Top Status Bar (Fixed height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { themeState.isDarkMode = !themeState.isDarkMode }) {
                        Icon(
                            imageVector = if (themeState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onNavigateToInbox) {
                        BadgedBox(badge = {
                            // TODO: Add unread count here
                        }) {
                            Icon(Icons.Default.Email, contentDescription = "Inbox", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GPS COORDS", style = MaterialTheme.typography.labelSmall)
                    Text(locationStr, color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isFireMode) "🔥" else "🌊", fontSize = 20.sp)
                    Switch(
                        checked = !isFireMode,
                        onCheckedChange = { themeState.isFireMode = !it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            // 2. CONDITIONAL UI BLOCK
            if (!isFireMode) {
                // FLOOD MODE: Full Operational Dashboard

                // A. SOS & Emergency Row (Fixed height)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { meshViewModel.sendSosBroadcast() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("🚨 SOS MESH BROADCAST", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmergencyButton("POLICE", Color(0xFF002366), Modifier.weight(1f)) {
                            meshViewModel.sendCategorizedAlert("POLICE")
                        }
                        EmergencyButton("MEDICAL", Color(0xFF2E7D32), Modifier.weight(1f)) {
                            meshViewModel.sendCategorizedAlert("MEDICAL")
                        }
                        EmergencyButton(
                            "FIRE/RESCUE", 
                            Color(0xFF0277BD), 
                            Modifier.weight(1f)
                        ) {
                            meshViewModel.sendCategorizedAlert("FIRE_RESCUE")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // B. The Offline Map Section (Weight 1 - Strictly Middle)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.DarkGray)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                
                                val provider = GpsMyLocationProvider(ctx)
                                val locationOverlay = MyLocationNewOverlay(provider, this)
                                locationOverlay.enableMyLocation()
                                locationOverlay.enableFollowLocation()
                                overlays.add(locationOverlay)

                                // Bridge Map Location to Dashboard UI
                                provider.startLocationProvider(object : IMyLocationConsumer {
                                    override fun onLocationChanged(location: android.location.Location?, source: IMyLocationProvider?) {
                                        location?.let {
                                            meshViewModel.updateLocation(it.latitude, it.longitude)
                                        }
                                    }
                                })

                                controller.setZoom(15.0)
                                controller.setCenter(currentGeoPoint)
                                capturedMapView = this
                            }
                        },
                        update = { mapView ->
                            // Clear non-location overlays
                            val toRemove = mapView.overlays.filterIsInstance<Marker>()
                            mapView.overlays.removeAll(toRemove)

                            // Add markers for peers
                            peers.forEach { peer ->
                                if (peer.latitude != null && peer.longitude != null) {
                                    val marker = Marker(mapView)
                                    marker.position = GeoPoint(peer.latitude, peer.longitude)
                                    marker.title = peer.deviceName
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    mapView.overlays.add(marker)
                                }
                            }
                            mapView.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Diagnostic Control (Bottom Right)
                    FloatingActionButton(
                        onClick = onNavigateToHealth,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = "Mesh Health")
                    }
                }

                // C. Voice Mesh Panel (Fixed height at bottom)
                TargetOverrideRow(voiceViewModel)

                VoiceMeshControlPanel(
                    activeNodes = peers.size,
                    recordingState = recordingState,
                    voiceViewModel = voiceViewModel,
                    onStart = {
                        val check = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (check == PackageManager.PERMISSION_GRANTED) voiceViewModel.onHoldToTalkStart()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onRelease = { voiceViewModel.onHoldToTalkRelease(false) },
                    onCancel = { voiceViewModel.onHoldToTalkCancel() }
                )
            } else {
                // 3. FIRE MODE: Completely blank operational area
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun EmergencyButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun TargetOverrideRow(viewModel: BlockvoiceViewModel) {
    val targetId by viewModel.targetRecipientId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (targetId == null) {
            Text("📡 MODE: BROADCAST", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Yellow))
                Spacer(Modifier.width(8.dp))
                Text("🎯 TARGET LOCKED: ${targetId?.take(8)}", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
            }
        }

        TextButton(onClick = { if (targetId == null) showDialog = true else viewModel.clearTarget() }) {
            Text(if (targetId == null) "SET TARGET" else "CLEAR", fontSize = 10.sp)
        }
    }

    if (showDialog) {
        var first by remember { mutableStateOf("") }
        var middle by remember { mutableStateOf("") }
        var last by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Direct Message Target") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = first, onValueChange = { first = it }, label = { Text("First Name") })
                    OutlinedTextField(value = middle, onValueChange = { middle = it }, label = { Text("Middle Name") })
                    OutlinedTextField(value = last, onValueChange = { last = it }, label = { Text("Last Name") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setTarget(first, middle, last)
                    showDialog = false
                }) { Text("LOCK") }
            }
        )
    }
}

@Composable
fun VoiceMeshControlPanel(
    activeNodes: Int,
    recordingState: RecordingState,
    voiceViewModel: BlockvoiceViewModel,
    onStart: () -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit
) {
    val themeState = LocalPhoenixThemeState.current
    val accentColor = if (themeState.isFireMode) Color(0xFFFF4500) else Color(0xFF00CED1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = voiceViewModel.textInput,
                onValueChange = { voiceViewModel.textInput = it },
                placeholder = { Text("Type mesh message...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(onClick = { voiceViewModel.sendTextMessage() }) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = accentColor)
                    }
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
            Spacer(Modifier.width(8.dp))
            Text("🟢 Mesh Nodes: $activeNodes Active", style = MaterialTheme.typography.bodySmall)
        }

        // PTT Button
        PttButton(
            accentColor = accentColor,
            recordingState = recordingState,
            onStart = onStart,
            onRelease = onRelease,
            onCancel = onCancel
        )

        Text(
            text = if (recordingState == RecordingState.RECORDING) "<-- Swipe left to cancel" else "Hold button to speak",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun PttButton(
    accentColor: Color,
    recordingState: RecordingState,
    onStart: () -> Unit,
    onRelease: () -> Unit,
    onCancel: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val isRecording = recordingState == RecordingState.RECORDING
    
    val pulseScale by animateFloatAsState(
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = if (isRecording) {
            infiniteRepeatable(tween(500), RepeatMode.Reverse)
        } else {
            tween(200)
        },
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .scale(pulseScale)
            .clip(CircleShape)
            .background(if (isRecording) Color.Red else accentColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        onStart()
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isRecording) {
                            offsetX += dragAmount.x
                            if (offsetX < -120f) onCancel()
                        }
                    },
                    onDragEnd = {
                        if (isRecording) onRelease()
                        offsetX = 0f
                    },
                    onDragCancel = {
                        onCancel()
                        offsetX = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
    }
}
