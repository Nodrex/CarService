package com.nodrex.carservice

import android.companion.CompanionDeviceService
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat

class CarCompanionService : CompanionDeviceService() {

    override fun onDeviceAppeared(associationInfo: android.companion.AssociationInfo) {
        Toast.makeText(this, "Car service on (Companion)", Toast.LENGTH_SHORT).show()
        val serviceIntent = Intent(this, CarAutomationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onDeviceDisappeared(associationInfo: android.companion.AssociationInfo) {
        val serviceIntent = Intent(this, CarAutomationService::class.java)
        stopService(serviceIntent)
    }
}
