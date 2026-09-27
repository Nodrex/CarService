package com.nodrex.carservice.screens

import com.nodrex.carservice.viewmodel.BluetoothPickerViewModel
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@SuppressLint("MissingPermission")
@Composable
fun CarConnectScreen(viewModel: BluetoothPickerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingAssociationAddress by remember { mutableStateOf<String?>(null) }
    var pendingAssociationName by remember { mutableStateOf<String?>(null) }

    val cdmLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingAssociationAddress?.let { address ->
                viewModel.handleAssociationResult(address, pendingAssociationName)
            }
        }
        pendingAssociationAddress = null
        pendingAssociationName = null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDevices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Configured Vehicles",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.targetDeviceAddresses.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No vehicle configured yet. Select a device below.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val selectedDevices = uiState.bondedDevices.filter { 
                uiState.targetDeviceAddresses.contains(it.address) 
            }
            
            selectedDevices.forEach { device ->
                val isConnected = uiState.connectedDevices.contains(device.address)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = device.name ?: "Unknown Device",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray
                        ) {
                            Text(
                                text = if (isConnected) "Connected" else "Idle",
                                color = if (isConnected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Paired Devices",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.bondedDevices) { device ->
                val isSelected = uiState.targetDeviceAddresses.contains(device.address)
                val isConnected = uiState.connectedDevices.contains(device.address)
                
                ListItem(
                    headlineContent = { Text(device.name ?: "Unknown Device") },
                    supportingContent = { Text(device.address) },
                    trailingContent = {
                        Switch(
                            checked = isSelected,
                            onCheckedChange = { isChecking ->
                                if (isChecking) {
                                    pendingAssociationAddress = device.address
                                    pendingAssociationName = device.name
                                    viewModel.associateCompanionDevice(device.address, device.name) { intentSender ->
                                        cdmLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    }
                                } else {
                                    viewModel.toggleTargetDevice(device.address, device.name)
                                }
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
                    )
                )
                HorizontalDivider()
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "App Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                FeatureItem(icon = "🎵", title = "Auto-play Spotify", description = "Resumes your music playback the moment you connect.")
                FeatureItem(icon = "🗺️", title = "Launch Navigation", description = "Automatically opens Waze for easy routing.")
                FeatureItem(icon = "📱", title = "Wake Screen", description = "Turns on the display so you don't have to unlock your phone.")
                FeatureItem(icon = "🔦", title = "Shake-to-Flashlight", description = "Shake the phone while connected to toggle the flashlight.")
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        var showLogs by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.testTriggerAutomation() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Trigger")
            }
            OutlinedButton(
                onClick = { showLogs = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("View Logs")
            }
        }

        if (showLogs) {
            LogBottomSheet(onDismiss = { showLogs = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogBottomSheet(onDismiss: () -> Unit) {
    val logs by com.nodrex.carservice.util.AppLogger.logs.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("System Logs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = {
                    val logText = logs.joinToString("\n")
                    val clip = android.content.ClipData.newPlainText("CarConnect Logs", logText)
                    clipboardManager.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Logs copied!", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy All")
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


