package com.nodrex.carservice.services

import android.companion.CompanionDeviceService
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat

class CarCompanionService : CompanionDeviceService() {

    override fun onDeviceAppeared(associationInfo: android.companion.AssociationInfo) {
        com.nodrex.carservice.util.AppLogger.log("CarCompanionService: Device connected ($associationInfo)")
        val serviceIntent = Intent(this, CarAutomationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

        override fun onDeviceDisappeared(associationInfo: android.companion.AssociationInfo) {
        com.nodrex.carservice.util.AppLogger.log("CarCompanionService: Device disconnected ($associationInfo)")
        
        // Pause Media
        try {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE))
            com.nodrex.carservice.util.AppLogger.log("CarCompanionService: Paused media playback via AudioManager")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return to Home Screen (Simulate closing Waze)
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            com.nodrex.carservice.util.AppLogger.log("CarCompanionService: Returned to home screen")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val serviceIntent = Intent(this, CarAutomationService::class.java)
        stopService(serviceIntent)
    }
}




