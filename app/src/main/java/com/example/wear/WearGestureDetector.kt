package com.example.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Wear OS One-Handed Gestures Detector (單手手勢偵測引擎)
 *
 * Implements the gesture architecture outlined by the Wear OS One-Handed Gestures framework:
 * 1. Primary Action (主要操作): Triggered by Double-Pinch / Double Quick Tap pulse (雙指捏合 / 兩下脈衝)
 *    - Opens Voice Search on Home, triggers TTS / Refresh in Web view.
 * 2. Dismiss Action (關閉/返回操作): Triggered by Wrist Turn / Wrist Flick (手腕外翻 / 轉腕甩動)
 *    - Dismisses active dialogs or navigates back in browser history.
 */
class WearGestureDetector(
    private val context: Context,
    private val listener: GestureListener
) : SensorEventListener {

    interface GestureListener {
        /**
         * 觸發主要操作（例如雙指捏合/點擊兩下）：語音搜尋、朗讀、確認
         */
        fun onPrimaryAction()

        /**
         * 觸發關閉/返回操作（例如手腕翻轉）：返回上一頁、關閉彈窗
         */
        fun onDismissAction()
    }

    enum class Sensitivity(val label: String, val accelThreshold: Float, val gyroThreshold: Float) {
        LOW("低靈敏度", 18.0f, 6.5f),
        STANDARD("標準", 14.0f, 4.5f),
        HIGH("高靈敏度", 10.5f, 3.2f)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var isEnabled: Boolean = true
    var sensitivity: Sensitivity = Sensitivity.STANDARD

    private var isListening = false

    // Double-pulse (Double Pinch) detection state
    private var lastPulseTime: Long = 0L
    private var pulseCount: Int = 0
    private val doublePulseWindowMs = 650L // Window to detect second pinch/pulse
    private val minPulseIntervalMs = 120L  // Debounce between pulses

    // Cooldown state to prevent repeated triggers
    private var lastPrimaryTriggerTime: Long = 0L
    private var lastDismissTriggerTime: Long = 0L
    private val cooldownMs = 1000L

    // Wrist Turn / Flick tracking
    private var lastGyroTurnTime: Long = 0L

    fun start() {
        if (!isEnabled || isListening || sensorManager == null) return

        try {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            isListening = true
            Log.d(TAG, "WearGestureDetector started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register gesture sensors", e)
        }
    }

    fun stop() {
        if (!isListening || sensorManager == null) return
        try {
            sensorManager.unregisterListener(this)
            isListening = false
            pulseCount = 0
            Log.d(TAG, "WearGestureDetector stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister gesture sensors", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null) return

        val now = SystemClock.elapsedRealtime()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometer(event, now)
            }
            Sensor.TYPE_GYROSCOPE -> {
                handleGyroscope(event, now)
            }
        }
    }

    private fun handleAccelerometer(event: SensorEvent, now: Long) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate dynamic acceleration magnitude (excluding standard gravity 9.8m/s^2)
        val magnitude = sqrt(x * x + y * y + z * z)
        val deltaG = abs(magnitude - SensorManager.GRAVITY_EARTH)

        if (deltaG > sensitivity.accelThreshold) {
            if (now - lastPrimaryTriggerTime < cooldownMs) return

            if (now - lastPulseTime in minPulseIntervalMs..doublePulseWindowMs) {
                pulseCount++
                if (pulseCount >= 2) {
                    // Double Pinch / Double Tap detected!
                    pulseCount = 0
                    lastPrimaryTriggerTime = now
                    Log.i(TAG, "Double-Pinch (Primary Action) gesture detected! deltaG: $deltaG")
                    listener.onPrimaryAction()
                }
            } else if (now - lastPulseTime > doublePulseWindowMs) {
                // First pulse
                pulseCount = 1
                lastPulseTime = now
            }
        }
    }

    private fun handleGyroscope(event: SensorEvent, now: Long) {
        if (now - lastDismissTriggerTime < cooldownMs) return

        // Angular velocity in rad/s: Rotation around Y and Z axis (wrist flick / turn)
        val rotY = abs(event.values[1])
        val rotZ = abs(event.values[2])
        val maxRot = maxOf(rotY, rotZ)

        if (maxRot > sensitivity.gyroThreshold) {
            // Rapid wrist rotation / flick detected
            lastDismissTriggerTime = now
            lastGyroTurnTime = now
            pulseCount = 0 // Reset pinch detector to prevent confusion
            Log.i(TAG, "Wrist-Turn (Dismiss Action) gesture detected! rot: $maxRot")
            listener.onDismissAction()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    companion object {
        private const val TAG = "WearGestureDetector"
    }
}
