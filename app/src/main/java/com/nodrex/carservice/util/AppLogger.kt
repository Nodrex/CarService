package com.nodrex.carservice.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "CarConnectLogs"
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        val formattedMessage = "[$timestamp] $message"
        
        // Log to Logcat
        Log.d(TAG, formattedMessage)
        
        // Add to UI logs (keep last 500 lines to avoid memory issues)
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(formattedMessage)
        if (currentLogs.size > 500) {
            currentLogs.removeAt(0)
        }
        _logs.value = currentLogs
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
