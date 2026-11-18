package com.example.neuro_sense

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

internal class MotionCapture(context: Context) {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val linearSensor = manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val rawSensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var listener: SensorEventListener? = null

    val available: Boolean get() = linearSensor != null || rawSensor != null

    fun start(onSample: (Long, MotionAxes, Boolean) -> Unit): MotionSource? {
        stop()
        if (linearSensor != null && register(linearSensor, null, onSample)) {
            return MotionSource.LinearAcceleration
        }
        if (rawSensor != null && register(rawSensor, GravityFilter(), onSample)) {
            return MotionSource.FilteredAccelerometer
        }
        return null
    }

    private fun register(
        sensor: Sensor,
        filter: GravityFilter?,
        onSample: (Long, MotionAxes, Boolean) -> Unit,
    ): Boolean {
        val candidate = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

            override fun onSensorChanged(event: SensorEvent) {
                if (listener !== this) return
                val (x, y, z) = event.values
                if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return
                val axes = if (filter == null) MotionAxes(x, y, z)
                else filter.removeGravity(event.timestamp, x, y, z) ?: return
                val range = sensor.maximumRange
                val nearLimit = range.isFinite() && range > 0f &&
                    maxOf(abs(x), abs(y), abs(z)) >= range * 0.99f
                onSample(event.timestamp, axes, nearLimit)
            }
        }
        val registered = try {
            manager?.registerListener(candidate, sensor, 20_000) == true
        } catch (_: SecurityException) {
            false
        }
        if (registered) listener = candidate
        return registered
    }

    fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }
}
