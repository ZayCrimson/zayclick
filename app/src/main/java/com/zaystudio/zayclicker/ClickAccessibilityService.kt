package com.zaystudio.zayclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class ClickAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var clickRunnable: Runnable? = null
    private var holdActive = false

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopAll() }
    override fun onDestroy() {
        stopAll()
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun handle(action: Action, x: Int, y: Int, interval: Int) {
        when (action) {
            Action.START_CLICK -> startClick(x, y, interval)
            Action.STOP_CLICK -> stopClick()
            Action.START_HOLD -> startHold(x, y)
            Action.STOP_HOLD -> stopHold()
            Action.STOP_ALL -> stopAll()
        }
    }

    private fun startClick(x: Int, y: Int, interval: Int) {
        stopClick()
        val task = object : Runnable {
            override fun run() {
                tap(x, y)
                clickRunnable = this
                handler.postDelayed(this, interval.toLong().coerceAtLeast(40L))
            }
        }
        clickRunnable = task
        handler.post(task)
    }

    private fun stopClick() {
        clickRunnable?.let(handler::removeCallbacks)
        clickRunnable = null
    }

    private fun startHold(x: Int, y: Int) {
        stopHold()
        holdActive = true
        // Accessibility gestures have a finite duration. Re-dispatching a hold can
        // cancel other gestures, so this is a standalone hold implementation.
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()); lineTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60_000))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCancelled(gestureDescription: GestureDescription?) {
                holdActive = false
            }
        }, null)
    }

    private fun stopHold() {
        if (!holdActive) return
        holdActive = false
        // An empty/cancel gesture is not available in the public API. A short
        // gesture dispatch replaces the active gesture on supported Android builds.
        val p = Path().apply { moveTo(0f, 0f); lineTo(0f, 0f) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 1)).build(), null, null)
    }

    private fun tap(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()); lineTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 45))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun stopAll() {
        stopClick()
        stopHold()
    }

    companion object { @Volatile var instance: ClickAccessibilityService? = null }
}
