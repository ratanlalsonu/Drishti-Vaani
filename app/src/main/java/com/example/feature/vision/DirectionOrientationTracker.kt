package com.example.feature.vision

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Tracks device pointing direction and orientation changes for visually impaired users.
 * Senses when user turns or sweeps their phone to a new direction (> 22 degrees),
 * stabilizes the view, and triggers a one-time spatial announcement (front, left, right).
 */
class DirectionOrientationTracker(
    context: Context,
    private val onDirectionMoved: () -> Unit,
    private val onDirectionSettled: (azimuth: Float, pitch: Float, directionNameHi: String, directionNameEn: String) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val accelReading = FloatArray(3)
    private val magnetReading = FloatArray(3)
    private var hasAccel = false
    private var hasMagnet = false

    private val trackerScope = CoroutineScope(Dispatchers.Main + Job())
    private var settleJob: Job? = null

    // Angle thresholds - set high enough to ignore natural hand tremors while detecting deliberate direction turns
    private val azimuthShiftThresholdDegrees = 30.0f
    private val pitchShiftThresholdDegrees = 24.0f
    private val settleDelayMs = 750L

    @Volatile
    var currentAzimuth: Float = 0f
        private set
    @Volatile
    var currentPitch: Float = 0f
        private set

    @Volatile
    var lastSettledAzimuth: Float = -999f
        private set
    @Volatile
    var lastSettledPitch: Float = -999f
        private set

    private var isListening = false
    private var isCurrentlyMoving = false

    fun start() {
        if (isListening || sensorManager == null) return
        isListening = true

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        settleJob?.cancel()
        settleJob = null
        isCurrentlyMoving = false
        lastSettledAzimuth = -999f
        lastSettledPitch = -999f
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isListening) return

        var hasOrientation = false

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            hasOrientation = true
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelReading, 0, accelReading.size)
            hasAccel = true
            if (hasMagnet) {
                hasOrientation = SensorManager.getRotationMatrix(rotationMatrix, null, accelReading, magnetReading)
                if (hasOrientation) SensorManager.getOrientation(rotationMatrix, orientationAngles)
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetReading, 0, magnetReading.size)
            hasMagnet = true
            if (hasAccel) {
                hasOrientation = SensorManager.getRotationMatrix(rotationMatrix, null, accelReading, magnetReading)
                if (hasOrientation) SensorManager.getOrientation(rotationMatrix, orientationAngles)
            }
        }

        if (hasOrientation) {
            val azimuthRad = orientationAngles[0]
            val pitchRad = orientationAngles[1]

            val rawAzimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
            val normalizedAzimuth = (rawAzimuthDeg + 360f) % 360f
            val normalizedPitch = Math.toDegrees(pitchRad.toDouble()).toFloat()

            checkOrientationShift(normalizedAzimuth, normalizedPitch)
        }
    }

    private fun checkOrientationShift(azimuth: Float, pitch: Float) {
        currentAzimuth = azimuth
        currentPitch = pitch

        // First initialization
        if (lastSettledAzimuth < -500f) {
            lastSettledAzimuth = azimuth
            lastSettledPitch = pitch
            triggerSettleSchedule(azimuth, pitch)
            return
        }

        val azimuthDiff = calculateAngleDifference(lastSettledAzimuth, azimuth)
        val pitchDiff = abs(lastSettledPitch - pitch)

        if (azimuthDiff >= azimuthShiftThresholdDegrees || pitchDiff >= pitchShiftThresholdDegrees) {
            if (!isCurrentlyMoving) {
                isCurrentlyMoving = true
                onDirectionMoved()
            }
            // Reset debounce timer on ongoing movement
            triggerSettleSchedule(azimuth, pitch)
        }
    }

    private fun triggerSettleSchedule(targetAzimuth: Float, targetPitch: Float) {
        settleJob?.cancel()
        settleJob = trackerScope.launch {
            delay(settleDelayMs)
            lastSettledAzimuth = targetAzimuth
            lastSettledPitch = targetPitch
            isCurrentlyMoving = false
            val (hiName, enName) = getDirectionNames(targetAzimuth)
            onDirectionSettled(targetAzimuth, targetPitch, hiName, enName)
        }
    }

    /**
     * Manually triggers a direction shift (useful for fallback or user actions)
     */
    fun triggerManualShift() {
        isCurrentlyMoving = true
        onDirectionMoved()
        triggerSettleSchedule(currentAzimuth, currentPitch)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        fun calculateAngleDifference(a: Float, b: Float): Float {
            var diff = abs(a - b) % 360f
            if (diff > 180f) {
                diff = 360f - diff
            }
            return diff
        }

        fun getDirectionNames(azimuth: Float): Pair<String, String> {
            val norm = (azimuth % 360f + 360f) % 360f
            return when {
                norm >= 337.5f || norm < 22.5f -> Pair("उत्तर दिशा (North)", "North direction")
                norm < 67.5f -> Pair("उत्तर-पूर्व (North-East)", "North-East direction")
                norm < 112.5f -> Pair("पूर्व दिशा (East)", "East direction")
                norm < 157.5f -> Pair("दक्षिण-पूर्व (South-East)", "South-East direction")
                norm < 202.5f -> Pair("दक्षिण दिशा (South)", "South direction")
                norm < 247.5f -> Pair("दक्षिण-पश्चिम (South-West)", "South-West direction")
                norm < 292.5f -> Pair("पश्चिम दिशा (West)", "West direction")
                else -> Pair("उत्तर-पश्चिम (North-West)", "North-West direction")
            }
        }
    }
}
