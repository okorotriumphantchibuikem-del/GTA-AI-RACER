package com.gtairacer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
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
            this.text = text; textSize = size; setTextColor(Color.WHITE)
            setPadding(0, 14, 0, 14)
        }
        val title = label("GTA AI RACER", 28f)
        title.gravity = Gravity.CENTER
        status = label("Stage 1: screen-capture connection test\n\nThis starter build does not drive the car yet. First grant screen capture, then we will add on-device road and checkpoint recognition.")
        val capture = Button(this).apply {
            text = "1. Enable game screen capture"
            setOnClickListener {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
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
        layout.addView(title); layout.addView(label("Your phone: Android\nMode: local processing — no laptop or cloud required"))
        layout.addView(capture); layout.addView(accessibility); layout.addView(openGame); layout.addView(status)
        setContentView(layout)
    }

    @Deprecated("Deprecated by Android but supported for this starter")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode); putExtra("projectionData", data)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
            status.text = "Screen capture permission granted. Capture service started.\n\nNext milestone: inspect captured frames and identify the road, player car and race checkpoints."
        } else if (requestCode == captureRequest) status.text = "Screen capture was not approved. Tap the capture button and allow it."
    }
}
