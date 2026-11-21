package com.gtairacer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    private val captureRequest = 4101
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
            setBackgroundColor(Color.rgb(12, 17, 28))
        }
        fun label(text: String, size: Float = 16f) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 14, 0, 14)
        }

        val title = label("GTA AI RACER", 28f).apply { gravity = Gravity.CENTER }
        status = label(
            "Stage 1: screen-capture connection test\n\n" +
            "Tap the capture button and wait for Android's system confirmation. " +
            "Choose Start now to share the game screen. This starter build does not drive the car yet."
        )

        val capture = Button(this).apply {
            text = "1. Enable game screen capture"
            setOnClickListener {
                isEnabled = false
                status.text = "Waiting for Android screen-capture permission…\n\nWhen the system dialog appears, choose Start now. If it closes, return here and tap the button again."
                try {
                    val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
                } catch (error: Exception) {
                    status.text = "Could not open Android screen-capture permission: ${error.javaClass.simpleName}: ${error.message}"
                } finally {
                    isEnabled = true
                }
            }
        }

        val accessibility = Button(this).apply {
            text = "2. Open accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        val openGame = Button(this).apply {
            text = "3. Open GTA San Andreas"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("com.rockstargames.gtasa")
                if (launch != null) startActivity(launch)
                else status.text = "GTA app not found under the standard package name. Open GTA manually; some editions use a different package."
            }
        }

        layout.addView(title)
        layout.addView(label("Your phone: Android\nMode: local processing — no laptop or cloud required"))
        layout.addView(capture)
        layout.addView(accessibility)
        layout.addView(openGame)
        layout.addView(status)
        setContentView(layout)
    }

    @Deprecated("Deprecated by Android but supported for this starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequest) return

        if (resultCode == RESULT_OK && data != null) {
            try {
                val service = Intent(this, CaptureService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("projectionData", data)
                }
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
                status.text = "Screen-capture permission granted. Starting capture service…"
            } catch (error: Exception) {
                status.text = "Permission was granted, but capture service could not start: ${error.javaClass.simpleName}: ${error.message}"
            }
        } else {
            status.text = "Android closed or denied the screen-capture request. Tap button 1 again and choose Start now on the system dialog."
        }
    }
}
