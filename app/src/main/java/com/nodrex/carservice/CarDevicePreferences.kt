package com.nodrex.carservice

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "car_device_prefs")

class CarDevicePreferences(private val context: Context) {
    
    companion object {
        private val TARGET_DEVICE_ADDRESSES = stringSetPreferencesKey("target_device_addresses")
        private val TARGET_DEVICE_NAMES = stringSetPreferencesKey("target_device_names")
    }

    val targetDeviceAddresses: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[TARGET_DEVICE_ADDRESSES] ?: emptySet()
        }

    val targetDeviceNames: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[TARGET_DEVICE_NAMES] ?: emptySet()
        }

    suspend fun toggleTargetDevice(address: String, name: String?) {
        context.dataStore.edit { preferences ->
            val currentAddresses = preferences[TARGET_DEVICE_ADDRESSES] ?: emptySet()
            val currentNames = preferences[TARGET_DEVICE_NAMES] ?: emptySet()
            
            if (currentAddresses.contains(address)) {
                preferences[TARGET_DEVICE_ADDRESSES] = currentAddresses - address
                if (name != null) {
                    preferences[TARGET_DEVICE_NAMES] = currentNames - name
                }
            } else {
                preferences[TARGET_DEVICE_ADDRESSES] = currentAddresses + address
                if (name != null) {
                    preferences[TARGET_DEVICE_NAMES] = currentNames + name
                }
            }
        }
    }
}
