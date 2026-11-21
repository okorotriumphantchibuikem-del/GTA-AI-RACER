package com.gtairacer

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper

/**
 * Touch-input adapter for the default GTA San Andreas mobile layout.
 * Coordinates are normalized and must be calibrated against the user's HUD.
 */
class RaceInputController(private val service: GameAccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false

    fun start() { running = true }

    fun stop() {
        if (running) dispatchPedalRelease()
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    fun isRunning() = running

    /** steer: -1 = left, 0 = straight, +1 = right. */
    fun apply(steer: Float, accelerate: Boolean = true) {
        if (!running) return
        val w = service.resources.displayMetrics.widthPixels.toFloat()
        val h = service.resources.displayMetrics.heightPixels.toFloat()
        val centerX = w * 0.19f
        val centerY = h * 0.69f
        val travel = (w * 0.10f) * steer.coerceIn(-1f, 1f)
        val steeringPath = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX + travel, centerY)
        }
        val strokes = ArrayList<GestureDescription.StrokeDescription>()
        strokes.add(GestureDescription.StrokeDescription(steeringPath, 0, 180))
        if (accelerate) {
            val pedalPath = Path().apply {
                moveTo(w * 0.94f, h * 0.78f)
                lineTo(w * 0.94f, h * 0.78f)
            }
            strokes.add(GestureDescription.StrokeDescription(pedalPath, 0, 300))
        }
        service.dispatchControllerGesture(
            GestureDescription.Builder().apply { strokes.forEach { addStroke(it) } }.build()
        )
    }

    fun releasePedal() {
        if (!running) return
        dispatchPedalRelease()
    }

    private fun dispatchPedalRelease() {
        val w = service.resources.displayMetrics.widthPixels.toFloat()
        val h = service.resources.displayMetrics.heightPixels.toFloat()
        val path = Path().apply { moveTo(w * 0.94f, h * 0.78f) }
        service.dispatchControllerGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
                .build()
        )
    }
}
