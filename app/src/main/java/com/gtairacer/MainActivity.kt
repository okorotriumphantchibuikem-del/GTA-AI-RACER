package com.gtairacer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val captureRequest = 4101
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 36, 30, 30)
            setBackgroundColor(Color.rgb(12, 17, 28))
        }
        fun label(value: String, size: Float = 16f, color: Int = Color.WHITE) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                setPadding(0, 12, 0, 12)
            }

        content.addView(label("GTA AI RACER", 28f).apply { gravity = Gravity.CENTER })
        content.addView(label(
            "AUTONOMOUS DRIVING · EXPERIMENTAL\n" +
            "This build analyses captured frames locally and tries conservative steering when it detects a road. " +
            "It is an early visual heuristic, not a trained GTA AI. Keep your finger ready to stop it.",
            14f, Color.LTGRAY
        ))

        content.addView(actionButton("1. ENABLE / RESTART SCREEN CAPTURE") {
            if (CaptureService.captureActive) {
                status.text = "Capture is already active. No need to request permission again."
            } else {
                status.text = "Waiting for Android screen-sharing permission… Choose Start now."
                try {
                    val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
                } catch (error: Exception) {
                    status.text = "Could not request capture: ${error.javaClass.simpleName}: ${error.message}"
                }
            }
        })
        content.addView(actionButton("2. START AI DRIVING") {
            if (!CaptureService.captureActive) {
                status.text = "Screen capture is not active. Enable capture first."
            } else if (CaptureService.enableAutoDrive()) {
                status.text = "AI driving started. ${CaptureService.lastVisionStatus}"
            } else {
                status.text = "Accessibility service is not connected. Open accessibility settings and enable GTA AI RACER."
            }
        })
        content.addView(actionButton("3. STOP AI DRIVING", Color.rgb(150, 45, 45)) {
            CaptureService.disableAutoDrive()
            status.text = "AI driving stopped. ${CaptureService.lastVisionStatus}"
        })
        content.addView(actionButton("4. OPEN ACCESSIBILITY SETTINGS") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(actionButton("5. OPEN GTA SAN ANDREAS") {
            val launch = packageManager.getLaunchIntentForPackage("com.rockstargames.gtasa")
            if (launch != null) startActivity(launch)
            else status.text = "Game package not detected. Open GTA San Andreas manually."
        })
        content.addView(actionButton("REFRESH STATUS") { refreshStatus() })

        status = label("Status: waiting for setup…", 15f, Color.rgb(150, 230, 180))
        content.addView(status)
        content.addView(label(
            "TESTING NOTES\n" +
            "• Use a normal driving/racing situation, not menus or cutscenes.\n" +
            "• The current detector looks for broad grey road-like regions. It may misread scenery, shadows or HUD elements.\n" +
            "• If it behaves unexpectedly, tap STOP AI DRIVING immediately.\n" +
            "• Steering and accelerator touch positions are provisional and may need calibration for your GTA control layout.",
            13f, Color.LTGRAY
        ))
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
    }

    private fun actionButton(textValue: String, backgroundColor: Int = Color.rgb(38, 75, 120), action: () -> Unit) =
        Button(this).apply {
            text = textValue
            setTextColor(Color.WHITE)
            setBackgroundColor(backgroundColor)
            setOnClickListener { action() }
        }

    private fun refreshStatus() {
        status.text = "Capture: ${if (CaptureService.captureActive) "ACTIVE" else "OFF"}\n" +
            "AI driver: ${if (CaptureService.autoDriveEnabled) "RUNNING" else "STOPPED"}\n" +
            CaptureService.lastVisionStatus +
            "\nAccessibility: ${if (GameAccessibilityService.instance != null) "CONNECTED" else "NOT CONNECTED"}"
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    @Deprecated("Deprecated by Android but supported for this project")
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
                status.text = "Permission granted. Starting the capture and vision service…"
            } catch (error: Exception) {
                status.text = "Could not start capture: ${error.javaClass.simpleName}: ${error.message}"
            }
        } else {
            status.text = "Screen sharing was cancelled. Tap capture and choose Start now."
        }
    }
}
