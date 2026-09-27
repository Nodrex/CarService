package com.nodrex.carservice.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.nodrex.carservice.util.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RouterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log("RouterActivity: Screen wake sequence initiated")
        
        wakeScreen()
        
        lifecycleScope.launch {
            launchSpotify()
            delay(1500)
            launchWaze()
            AppLogger.log("RouterActivity: Finishing")
            finish()
        }
    }

    private fun wakeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun launchSpotify() {
        AppLogger.log("RouterActivity: Launching Spotify...")
        val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            `package` = "com.spotify.music"
        }
        sendOrderedBroadcast(downIntent, null)

        val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
            `package` = "com.spotify.music"
        }
        sendOrderedBroadcast(upIntent, null)

        // Fallback: Launch the app to ensure it starts
        try {
            val fallbackIntent = packageManager.getLaunchIntentForPackage("com.spotify.music")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (fallbackIntent != null) {
                startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchWaze() {
        AppLogger.log("RouterActivity: Launching Waze...")
        try {
            val wazeIntent = packageManager.getLaunchIntentForPackage("com.waze")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (wazeIntent != null) {
                startActivity(wazeIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
