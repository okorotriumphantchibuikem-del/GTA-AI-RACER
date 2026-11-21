package com.gtairacer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent

class GameAccessibilityService : AccessibilityService() {
    private var controller: RaceInputController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        controller = RaceInputController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Later milestone: use app/window state to ensure inputs target the game only.
    }

    override fun onInterrupt() { controller?.stop() }

    fun startRacingInput() { controller?.start() }
    fun stopRacingInput() { controller?.stop() }
    fun steerAndAccelerate(value: Float) { controller?.apply(value, true) }

    fun dispatchControllerGesture(gesture: GestureDescription): Boolean =
        dispatchGesture(gesture, null, null)
}
