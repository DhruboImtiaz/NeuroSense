package com.example.neuro_sense

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMeasurementTest {
    private fun MotionWindow.addScalar(timestamp: Long, value: Float) =
        add(timestamp, MotionAxes(value, 0f, 0f), false)

    private val start = 5_000_000_000L

    @Test
    fun constantAccelerationIsMeasuredInSensorUnits() {
        val window = MotionWindow(start)
        repeat(500) { index -> window.addScalar(start + index * 20_000_000L, 0.4f) }

        val result = requireNotNull(window.finish())
        assertEquals(0.4, result.rms, 1e-6)
        assertEquals(0.4f, result.peak, 0f)
        assertEquals(500, result.sampleCount)
        assertEquals(100, result.trace.size)
        assertTrue(result.trace.zipWithNext().all { (a, b) -> a.seconds < b.seconds })
    }

    @Test
    fun irregularSamplingUsesElapsedTimeAndKeepsShortPeaks() {
        val window = MotionWindow(start)
        repeat(250) { index -> window.addScalar(start + index * 20_000_000L, 1f) }
        repeat(500) { index -> window.addScalar(start + 5_000_000_000L + index * 10_000_000L, 3f) }

        val result = requireNotNull(window.finish())
        assertEquals(sqrt(5.0), result.rms, 0.004)
        assertEquals(3f, result.peak, 0f)
        assertEquals(750, result.sampleCount)
    }

    @Test
    fun sparseAndInterruptedCapturesAreRejected() {
        assertNull(MotionWindow(start).finish())

        val sparse = MotionWindow(start)
        repeat(10) { index -> sparse.addScalar(start + index * 1_000_000_000L, 1f) }
        assertNull(sparse.finish())

        val interrupted = MotionWindow(start)
        repeat(500) { index ->
            if (index !in 200..250) interrupted.addScalar(start + index * 20_000_000L, 1f)
        }
        assertNull(interrupted.finish())

        val endedEarly = MotionWindow(start)
        repeat(350) { index -> endedEarly.addScalar(start + index * 20_000_000L, 1f) }
        assertNull(endedEarly.finish())
    }

    @Test
    fun ignoresWarmupDuplicateAndInvalidReadings() {
        val window = MotionWindow(start)
        window.addScalar(start - 1, 99f)
        window.addScalar(start + CAPTURE_NANOS, 99f)
        repeat(500) { index ->
            val timestamp = start + index * 20_000_000L
            window.addScalar(timestamp, Float.NaN)
            window.addScalar(timestamp, 0f)
            window.addScalar(timestamp, 99f)
        }

        val result = requireNotNull(window.finish())
        assertEquals(500, result.sampleCount)
        assertEquals(0.0, result.rms, 0.0)
        assertEquals(0f, result.peak, 0f)
    }

    @Test
    fun gravityFilterZerosOutAStationaryTiltedPhone() {
        val filter = GravityFilter()
        assertNull(filter.removeGravity(start, 3f, 4f, 8f))
        assertEquals(0f, filter.removeGravity(start + 20_000_000L, 3f, 4f, 8f)?.magnitude ?: -1f, 0f)
        assertTrue(requireNotNull(filter.removeGravity(start + 40_000_000L, 4f, 4f, 8f)).magnitude > 0.9f)
    }
}
