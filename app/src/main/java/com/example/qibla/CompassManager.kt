package com.example.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassState(
    val azimuthDegrees: Float = 0f,
    val qiblaBearingDegrees: Float = 0f,
    val needleAngleDegrees: Float = 0f,
    val isFacingQibla: Boolean = false,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasCompassSensor: Boolean = true
)

class CompassManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassState = MutableStateFlow(CompassState())
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private var targetQiblaBearing: Float = 0f

    // Sensor smoothing filter buffers
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var lastAcc = FloatArray(3)
    private var lastMag = FloatArray(3)
    private var hasAcc = false
    private var hasMag = false
    private var smoothedAzimuth = 0f

    fun setQiblaBearing(bearing: Double) {
        targetQiblaBearing = bearing.toFloat()
        updateNeedle(smoothedAzimuth)
    }

    fun startListening() {
        if (sensorManager == null) {
            _compassState.value = _compassState.value.copy(hasCompassSensor = false)
            return
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            _compassState.value = _compassState.value.copy(hasCompassSensor = true)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            _compassState.value = _compassState.value.copy(hasCompassSensor = true)
        } else {
            _compassState.value = _compassState.value.copy(hasCompassSensor = false)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var azimuth = smoothedAzimuth

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAcc, 0, 3)
            hasAcc = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMag, 0, 3)
            hasMag = true
        }

        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR && hasAcc && hasMag) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAcc, lastMag)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            }
        }

        // Normalize 0..360
        val normalizedAzimuth = (azimuth + 360f) % 360f

        // Exponential smoothing filter to prevent needle jitter
        val alpha = 0.15f
        smoothedAzimuth = interpolateAngle(smoothedAzimuth, normalizedAzimuth, alpha)

        updateNeedle(smoothedAzimuth)
    }

    private fun updateNeedle(azimuth: Float) {
        val needleAngle = (targetQiblaBearing - azimuth + 360f) % 360f
        val angleDiff = abs(needleAngle.let { if (it > 180f) 360f - it else it })
        val isFacing = angleDiff <= 4f

        _compassState.value = _compassState.value.copy(
            azimuthDegrees = azimuth,
            qiblaBearingDegrees = targetQiblaBearing,
            needleAngleDegrees = needleAngle,
            isFacingQibla = isFacing
        )
    }

    private fun interpolateAngle(from: Float, to: Float, alpha: Float): Float {
        var diff = (to - from + 180f) % 360f - 180f
        if (diff < -180f) diff += 360f
        return (from + diff * alpha + 360f) % 360f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _compassState.value = _compassState.value.copy(accuracy = accuracy)
    }
}
