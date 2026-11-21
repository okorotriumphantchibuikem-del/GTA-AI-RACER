package com.gtairacer

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper

/**
 * Gesture layer for the user's described control scheme:
 * - left-side swipe to steer
 * - lower-right pedal to accelerate
 * Coordinates are normalized so they adapt to different screen sizes.
 *
 * This is an input primitive, not an autonomous racer by itself. A vision/
 * decision loop must supply steering values after detecting the road.
 */
class RaceInputController(private val service: GameAccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false

    fun start() { running = true }
    fun stop() { running = false; handler.removeCallbacksAndMessages(null) }
    fun isRunning() = running

    /**
     * steer: -1.0 = left, 0 = straight, +1.0 = right.
     * The swipe stays in the left-side steering region.
     */
    fun apply(steer: Float, accelerate: Boolean = true) {
        if (!running) return
        val w = service.resources.displayMetrics.widthPixels.toFloat()
        val h = service.resources.displayMetrics.heightPixels.toFloat()
        val centerX = w * 0.19f
        val centerY = h * 0.69f
        val travel = (w * 0.10f) * steer.coerceIn(-1f, 1f)
        val start = Path().apply { moveTo(centerX, centerY); lineTo(centerX + travel, centerY) }

        val strokes = ArrayList<GestureDescription.StrokeDescription>()
        strokes.add(GestureDescription.StrokeDescription(start, 0, 180))
        if (accelerate) {
            // Hold the on-screen accelerator near the lower-right corner.
            val pedal = Path().apply {
                moveTo(w * 0.94f, h * 0.78f)
                lineTo(w * 0.94f, h * 0.78f)
            }
            strokes.add(GestureDescription.StrokeDescription(pedal, 0, 260, true))
        }
        val gesture = GestureDescription.Builder().apply {
            strokes.forEach { addStroke(it) }
        }.build()
        service.dispatchControllerGesture(gesture)
    }

    fun releasePedal() {
        // A new gesture naturally ends the previous bounded press.
        if (!running) return
        val w = service.resources.displayMetrics.widthPixels.toFloat()
        val h = service.resources.displayMetrics.heightPixels.toFloat()
        val tap = Path().apply { moveTo(w * 0.94f, h * 0.78f) }
        service.dispatchControllerGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(tap, 0, 40))
                .build()
        )
    }
}
