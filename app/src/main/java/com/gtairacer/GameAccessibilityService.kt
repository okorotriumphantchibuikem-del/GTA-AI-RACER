package com.gtairacer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent

class GameAccessibilityService : AccessibilityService() {
    private var controller: RaceInputController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        controller = RaceInputController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { stopRacingInput() }

    override fun onDestroy() {
        stopRacingInput()
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun startRacingInput() { controller?.start() }
    fun stopRacingInput() { controller?.stop() }
    fun steerAndAccelerate(value: Float) { controller?.apply(value, true) }
    fun coastAndRelease() { controller?.releasePedal() }

    fun dispatchControllerGesture(gesture: GestureDescription): Boolean =
        dispatchGesture(gesture, null, null)

    companion object {
        @Volatile var instance: GameAccessibilityService? = null
            private set
    }
}
