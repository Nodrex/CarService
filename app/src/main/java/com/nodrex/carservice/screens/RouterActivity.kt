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
            
            // Retry Waze launch after 2 seconds to absolutely ensure it starts
            delay(2000)
            com.nodrex.carservice.util.AppLogger.log("RouterActivity: Retrying Waze launch just to be sure...")
            launchWaze()
            
            // Wait before finishing so Android doesn't cancel the activity transition
            delay(1500)
            com.nodrex.carservice.util.AppLogger.log("RouterActivity: Finishing")
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
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        } catch(e: Exception) {}

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
                AppLogger.log("RouterActivity: Launching Spotify UI")
                startActivity(fallbackIntent)
            } else {
                AppLogger.log("RouterActivity: ERROR - Spotify not found on device!")
            }
        } catch (e: Exception) {
            AppLogger.log("RouterActivity: ERROR - $e")
            e.printStackTrace()
        }
    }

    private fun launchWaze() {
        AppLogger.log("RouterActivity: Launching Waze...")
        try {
            // Strongest method: Deep link intent to force Waze open
            val deepLinkIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("waze://")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                `package` = "com.waze"
            }
            startActivity(deepLinkIntent)
            AppLogger.log("RouterActivity: Launched Waze via deep link")
        } catch (e: Exception) {
            AppLogger.log("RouterActivity: Deep link failed, trying package launch intent...")
            try {
                val wazeIntent = packageManager.getLaunchIntentForPackage("com.waze")?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (wazeIntent != null) {
                    AppLogger.log("RouterActivity: Launching Waze UI")
                    startActivity(wazeIntent)
                } else {
                    AppLogger.log("RouterActivity: ERROR - Waze not found on device!")
                }
            } catch (ex: Exception) {
                AppLogger.log("RouterActivity: ERROR - $ex")
                ex.printStackTrace()
            }
        }
    }
}

