package com.nodrex.carservice.viewmodel

import com.nodrex.carservice.data.CarDevicePreferences
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BluetoothUiState(
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val connectedDevices: Set<String> = emptySet(), // Device addresses
    val targetDeviceAddresses: Set<String> = emptySet()
)

@SuppressLint("MissingPermission")
class BluetoothPickerViewModel(
    private val context: Context,
    private val preferences: CarDevicePreferences
) : ViewModel() {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val _bondedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _connectedDevices = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<BluetoothUiState> = combine(
        _bondedDevices,
        _connectedDevices,
        preferences.targetDeviceAddresses
    ) { bonded, connected, targets ->
        BluetoothUiState(
            bondedDevices = bonded,
            connectedDevices = connected,
            targetDeviceAddresses = targets
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BluetoothUiState()
    )

    fun refreshDevices() {
        if (bluetoothAdapter == null) return

        _bondedDevices.value = bluetoothAdapter.bondedDevices?.toList() ?: emptyList()

        val connectedAddresses = mutableSetOf<String>()
        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                proxy.connectedDevices.forEach { device ->
                    connectedAddresses.add(device.address)
                }
                _connectedDevices.value = connectedAddresses.toSet()
                bluetoothAdapter.closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {}
        }

        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
    }

    fun associateCompanionDevice(
        address: String, 
        name: String?, 
        onIntentSender: (android.content.IntentSender) -> Unit
    ) {
        com.nodrex.carservice.util.AppLogger.log("ViewModel: Requesting CDM for $address")
        val cdm = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as android.companion.CompanionDeviceManager
        
        // If already associated, just save and start observing
        val existingAssociation = cdm.myAssociations.find { it.deviceMacAddress?.toString()?.uppercase() == address.uppercase() }
        if (existingAssociation != null) {
            cdm.startObservingDevicePresence(existingAssociation.deviceMacAddress.toString())
            toggleTargetDevice(address, name)
            return
        }

        val deviceFilter = android.companion.BluetoothDeviceFilter.Builder()
            .setAddress(address)
            .build()

        val request = android.companion.AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(true)
            .build()

        cdm.associate(request, context.mainExecutor, object : android.companion.CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: android.content.IntentSender) {
                onIntentSender(intentSender)
            }

            override fun onAssociationCreated(associationInfo: android.companion.AssociationInfo) {
                com.nodrex.carservice.util.AppLogger.log("ViewModel: CDM Association Created!")
                cdm.startObservingDevicePresence(associationInfo.deviceMacAddress.toString())
                toggleTargetDevice(address, name)
            }

            override fun onFailure(error: CharSequence?) {
                com.nodrex.carservice.util.AppLogger.log("ViewModel: CDM Association Failed: $error")
                // Fallback to just saving the target if CDM fails (some custom ROMs)
                toggleTargetDevice(address, name)
            }
        })
    }

    fun handleAssociationResult(address: String, name: String?) {
        com.nodrex.carservice.util.AppLogger.log("ViewModel: Requesting CDM for $address")
        val cdm = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as android.companion.CompanionDeviceManager
        val existingAssociation = cdm.myAssociations.find { it.deviceMacAddress?.toString()?.uppercase() == address.uppercase() }
        if (existingAssociation != null) {
            cdm.startObservingDevicePresence(existingAssociation.deviceMacAddress.toString())
        }
        toggleTargetDevice(address, name)
    }

    fun toggleTargetDevice(address: String, name: String?) {
        viewModelScope.launch {
            preferences.toggleTargetDevice(address, name)
        }
    }

    fun testTriggerAutomation() {
        com.nodrex.carservice.util.AppLogger.log("ViewModel: Firing Test Trigger!")
        val serviceIntent = android.content.Intent(context, com.nodrex.carservice.services.CarAutomationService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
    }
}

class BluetoothPickerViewModelFactory(
    private val context: Context,
    private val preferences: CarDevicePreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothPickerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothPickerViewModel(context, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



