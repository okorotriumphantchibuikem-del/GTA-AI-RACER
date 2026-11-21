package com.gtairacer

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var imageThread: HandlerThread? = null
    private var lastDecisionAt = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            disableAutoDrive()
            stopSelf()
            return START_NOT_STICKY
        }
        if (captureActive) return START_NOT_STICKY
        createNotification()
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = readProjectionIntent(intent)
        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(code, data)
            val activeProjection = projection ?: throw IllegalStateException("Android did not create a screen-capture session")
            activeProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    captureActive = false
                    disableAutoDrive()
                    virtualDisplay?.release()
                    virtualDisplay = null
                    reader?.close()
                    reader = null
                    stopSelf()
                }
            }, Handler(mainLooper))

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, android.graphics.PixelFormat.RGBA_8888, 3)
            imageThread = HandlerThread("GtaFrameVision").apply { start() }
            reader?.setOnImageAvailableListener({ source ->
                val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    if (!autoDriveEnabled) return@setOnImageAvailableListener
                    val now = System.currentTimeMillis()
                    if (now - lastDecisionAt < 350L) return@setOnImageAvailableListener
                    lastDecisionAt = now
                    val decision = RoadVision.analyze(image)
                    val driver = GameAccessibilityService.instance
                    if (decision != null && driver != null) {
                        driver.steerAndAccelerate(decision.steering)
                        lastVisionStatus = "Road detected · confidence ${(decision.confidence * 100).toInt()}% · steer ${(decision.steering * 100).toInt()}"
                    } else {
                        driver?.coastAndRelease()
                        lastVisionStatus = "Road not confidently detected · controls released"
                    }
                } catch (error: Exception) {
                    android.util.Log.e("GTA-AI-RACER", "Frame analysis failed", error)
                    lastVisionStatus = "Vision error: ${error.javaClass.simpleName}"
                    GameAccessibilityService.instance?.coastAndRelease()
                } finally {
                    image.close()
                }
            }, imageThread?.looper?.let { Handler(it) })

            virtualDisplay = activeProjection.createVirtualDisplay(
                "GTA-AI-RACER", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null
            )
            captureActive = true
            lastVisionStatus = "Screen capture active · AI is stopped"
            return START_NOT_STICKY
        } catch (error: Exception) {
            android.util.Log.e("GTA-AI-RACER", "Unable to start screen capture", error)
            captureActive = false
            stopSelf()
            return START_NOT_STICKY
        }
    }

    @Suppress("DEPRECATION")
    private fun readProjectionIntent(intent: Intent?): Intent? =
        if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("projectionData", Intent::class.java)
        else intent?.getParcelableExtra("projectionData")

    private fun createNotification() {
        val channel = "gta_capture"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(channel, "GTA AI RACER capture", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channel).setContentTitle("GTA AI RACER")
                .setContentText("Game screen capture and local vision are active")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("GTA AI RACER")
                .setContentText("Game screen capture and local vision are active")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
        }
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(7, notification)
    }

    override fun onDestroy() {
        captureActive = false
        disableAutoDrive()
        virtualDisplay?.release()
        reader?.setOnImageAvailableListener(null, null)
        reader?.close()
        imageThread?.quitSafely()
        projection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.gtairacer.STOP_CAPTURE"
        @Volatile var captureActive = false
            private set
        @Volatile var autoDriveEnabled = false
        @Volatile var lastVisionStatus = "Capture not started"

        fun enableAutoDrive(): Boolean {
            val service = GameAccessibilityService.instance ?: return false
            if (!captureActive) return false
            autoDriveEnabled = true
            service.startRacingInput()
            lastVisionStatus = "AI driving enabled · waiting for road detection"
            return true
        }

        fun disableAutoDrive() {
            autoDriveEnabled = false
            GameAccessibilityService.instance?.stopRacingInput()
            lastVisionStatus = "AI driving stopped"
        }
    }
}
