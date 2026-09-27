package com.nodrex.carservice.services

import com.nodrex.carservice.R
import com.nodrex.carservice.screens.RouterActivity
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class CarAutomationService : Service(), SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    
    private var isTorchOn = false
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD = 15f // Acceleration minus gravity
    private val DEBOUNCE_TIME_MS = 500L

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "CarServiceChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        com.nodrex.carservice.util.AppLogger.log("CarAutomationService: Service starting...")
        super.onCreate()
        setupSensorAndCamera()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()
        acquireWakeLock()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startForegroundServiceWithNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Car Automation Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Car Mode Active")
            .setContentText("Listening for shake gestures...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(
            NOTIFICATION_ID, 
            notification, 
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        try {
            com.nodrex.carservice.util.AppLogger.log("CarAutomationService: Starting RouterActivity directly via BAL exemption...")
            val routerIntent = Intent(this, RouterActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(routerIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSensorAndCamera() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CarService::ShakeWakeLock")
        wakeLock?.acquire()
        com.nodrex.carservice.util.AppLogger.log("CarAutomationService: WakeLock acquired")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val magnitudeWithoutGravity = acceleration - SensorManager.GRAVITY_EARTH

            if (magnitudeWithoutGravity > SHAKE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if ((currentTime - lastShakeTime) > DEBOUNCE_TIME_MS) {
                    lastShakeTime = currentTime
                    toggleTorch()
                }
            }
        }
    }

    private fun toggleTorch() {
        cameraId?.let { id ->
            try {
                isTorchOn = !isTorchOn
                com.nodrex.carservice.util.AppLogger.log("CarAutomationService: Toggling torch to $isTorchOn")
                cameraManager.setTorchMode(id, isTorchOn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        
        if (isTorchOn) {
            try {
                cameraId?.let { cameraManager.setTorchMode(it, false) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}






