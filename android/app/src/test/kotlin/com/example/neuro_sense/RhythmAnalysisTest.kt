package com.example.neuro_sense

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RhythmAnalysisTest {
    @Test
    fun estimatesFrequencyFromSignedAxesAndIrregularTimestamps() {
        val result = estimateRhythm(samples { time ->
            val wave = (0.4 * sin(2 * PI * 4.7 * time)).toFloat()
            MotionAxes(wave, wave, 0.1f)
        }, clipped = false)

        assertEquals(RhythmStatus.Detected, result.status)
        assertEquals(4.7, requireNotNull(result.frequencyHz), 0.10)
        assertEquals(sqrt(2.0) * 0.4 / sqrt(2.0), requireNotNull(result.accelerationRms), 0.06)
    }

    @Test
    fun reportsNoClearRhythmForStillnessNoiseAnImpactAndAShortBurst() {
        val random = Random(12)
        val cases = listOf<(Double) -> MotionAxes>(
            { MotionAxes(0.03f, 0f, 0f) },
            { MotionAxes(random.nextFloat() * 0.3f, 0f, 0f) },
            { t -> MotionAxes(if (t in 3.0..3.05) 4f else 0f, 0f, 0f) },
            { t -> MotionAxes(if (t < 4.9) sin(2 * PI * 5 * t).toFloat() else 0f, 0f, 0f) },
            { t -> MotionAxes(sin(2 * PI * 1.5 * t).toFloat(), 0f, 0f) },
        )
        for (signal in cases) {
            val result = estimateRhythm(samples(signal), clipped = false)
            assertEquals(RhythmStatus.NoClearRhythm, result.status)
            assertNull(result.frequencyHz)
        }
    }

    @Test
    fun checksSensorLimitsAndSamplingBeforeEstimatingFrequency() {
        val input = samples { t -> MotionAxes(sin(2 * PI * 5 * t).toFloat(), 0f, 0f) }
        assertEquals(RhythmStatus.SensorLimit, estimateRhythm(input, clipped = true).status)
        assertEquals(RhythmStatus.UnevenSampling, estimateRhythm(input.drop(15), clipped = false).status)
        assertEquals(
            RhythmStatus.UnevenSampling,
            estimateRhythm(input.filterIndexed { index, _ -> index !in 250..256 }, clipped = false).status,
        )
        assertEquals(
            RhythmStatus.UnevenSampling,
            estimateRhythm(input.filterIndexed { index, _ -> index % 2 == 0 }, clipped = false).status,
        )
    }

    private fun samples(signal: (Double) -> MotionAxes): List<MotionSample> =
        List(500) { index ->
            val timestamp = index * 20_000_000L + (index % 7) * 700_000L
            MotionSample(timestamp, signal(timestamp / 1e9))
        }
}
