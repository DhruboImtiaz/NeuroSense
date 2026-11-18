package com.example.neuro_sense

import kotlin.math.exp
import kotlin.math.sqrt

internal const val CAPTURE_SECONDS = 10
internal const val CAPTURE_NANOS = CAPTURE_SECONDS * 1_000_000_000L
internal const val PREP_SECONDS = 3

internal enum class MotionSource {
    LinearAcceleration,
    FilteredAccelerometer,
}

internal data class MotionPoint(val seconds: Float, val acceleration: Float)

internal data class MotionAxes(val x: Float, val y: Float, val z: Float) {
    val magnitude: Float
        get() = sqrt(x.toDouble() * x + y.toDouble() * y + z.toDouble() * z).toFloat()
}

internal data class MotionSample(val offsetNanos: Long, val axes: MotionAxes)

internal data class MotionReading(
    val rms: Double,
    val peak: Float,
    val sampleCount: Int,
    val trace: List<MotionPoint>,
    val samples: List<MotionSample>,
    val clipped: Boolean,
)

internal class GravityFilter {
    private var previousNanos: Long? = null
    private var gravityX = 0.0
    private var gravityY = 0.0
    private var gravityZ = 0.0

    fun removeGravity(nanos: Long, x: Float, y: Float, z: Float): MotionAxes? {
        val previous = previousNanos
        if (previous == null) {
            previousNanos = nanos
            gravityX = x.toDouble()
            gravityY = y.toDouble()
            gravityZ = z.toDouble()
            return null
        }
        if (nanos <= previous) return null

        val elapsedSeconds = (nanos - previous) / 1e9
        val weight = 1 - exp(-elapsedSeconds / 0.5)
        previousNanos = nanos
        gravityX += weight * (x - gravityX)
        gravityY += weight * (y - gravityY)
        gravityZ += weight * (z - gravityZ)
        return MotionAxes((x - gravityX).toFloat(), (y - gravityY).toFloat(), (z - gravityZ).toFloat())
    }
}

internal class MotionWindow(private val startNanos: Long) {
    private companion object {
        const val BIN_NANOS = 100_000_000L
        const val MAX_GAP_NANOS = 500_000_000L
        const val MIN_SAMPLES = 100
    }

    private val bins = arrayOfNulls<MotionPoint>((CAPTURE_NANOS / BIN_NANOS).toInt())
    private var firstNanos: Long? = null
    private var previousNanos: Long? = null
    private var previousSquare = 0.0
    private var integratedSquares = 0.0
    private var peak = 0f
    private val samples = mutableListOf<MotionSample>()
    private var clipped = false
    private var hasGap = false

    fun add(timestampNanos: Long, axes: MotionAxes, nearSensorLimit: Boolean) {
        val acceleration = axes.magnitude
        val offset = timestampNanos - startNanos
        if (offset !in 0 until CAPTURE_NANOS || !acceleration.isFinite() || acceleration < 0f) return

        val previous = previousNanos
        if (previous != null && timestampNanos <= previous) return

        val square = acceleration.toDouble() * acceleration
        if (previous == null) {
            firstNanos = timestampNanos
        } else {
            val gap = timestampNanos - previous
            hasGap = hasGap || gap > MAX_GAP_NANOS
            integratedSquares += (previousSquare + square) * 0.5 * gap
        }
        previousNanos = timestampNanos
        previousSquare = square
        peak = maxOf(peak, acceleration)
        samples += MotionSample(offset, axes)
        clipped = clipped || nearSensorLimit

        val index = (offset / BIN_NANOS).toInt()
        val currentPeak = bins[index]
        if (currentPeak == null || acceleration > currentPeak.acceleration) {
            bins[index] = MotionPoint((offset / 1e9).toFloat(), acceleration)
        }
    }

    fun trace(): List<MotionPoint> = bins.filterNotNull()

    fun finish(): MotionReading? {
        val first = firstNanos ?: return null
        val last = previousNanos ?: return null
        if (hasGap || samples.size < MIN_SAMPLES ||
            first - startNanos > MAX_GAP_NANOS ||
            startNanos + CAPTURE_NANOS - last > MAX_GAP_NANOS
        ) return null

        val rms = sqrt(integratedSquares / (last - first))
        return MotionReading(rms, peak, samples.size, trace(), samples.toList(), clipped)
    }
}
