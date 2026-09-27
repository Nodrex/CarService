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

    companion object {
        private const val SPOTIFY_MAX_RETRIES = 3
        private const val WAZE_MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log("RouterActivity: Screen wake sequence initiated")
        
        wakeScreen()
        
        lifecycleScope.launch {
            var spotifyStarted = false
            for (i in 1..SPOTIFY_MAX_RETRIES) {
                launchSpotify()
                delay(RETRY_DELAY_MS)
                if (isAppInForeground("com.spotify.music")) {
                    spotifyStarted = true
                    AppLogger.log("RouterActivity: Spotify successfully launched on try $i")
                    break
                }
                AppLogger.log("RouterActivity: Spotify not detected in foreground, retrying ($i/$SPOTIFY_MAX_RETRIES)...")
            }

            var wazeStarted = false
            for (i in 1..WAZE_MAX_RETRIES) {
                launchWaze()
                delay(RETRY_DELAY_MS)
                if (isAppInForeground("com.waze")) {
                    wazeStarted = true
                    com.nodrex.carservice.util.AppLogger.log("RouterActivity: Waze successfully launched on try $i")
                    break
                }
                com.nodrex.carservice.util.AppLogger.log("RouterActivity: Waze not detected in foreground, retrying ($i/$WAZE_MAX_RETRIES)...")
            }
            
            if (!spotifyStarted || !wazeStarted) {
                com.nodrex.carservice.util.AppLogger.log("RouterActivity: Failed to launch apps after all retries. Opening CarConnect fallback UI.")
                val fallbackIntent = Intent(this@RouterActivity, com.nodrex.carservice.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(fallbackIntent)
            }
            
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

    private fun isAppInForeground(packageName: String): Boolean {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 10000
            val events = usageStatsManager.queryEvents(startTime, endTime)
            var currentForegroundApp: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    currentForegroundApp = event.packageName
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED) {
                    if (currentForegroundApp == event.packageName) {
                        currentForegroundApp = null
                    }
                }
            }
            return currentForegroundApp == packageName
        } catch (e: Exception) {
            e.printStackTrace()
            return false
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



