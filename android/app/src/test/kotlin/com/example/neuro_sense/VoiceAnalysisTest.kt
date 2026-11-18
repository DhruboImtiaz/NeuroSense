package com.example.neuro_sense

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAnalysisTest {
    private val rate = 16_000

    @Test
    fun measuresFundamentalOfAHarmonicSignal() {
        for (frequency in listOf(85.0, 175.0, 330.0)) {
            val signal = audio { time ->
                0.22 * sin(2 * PI * frequency * time) + 0.28 * sin(4 * PI * frequency * time)
            }
            val result = analyzeVoice(signal, rate) as VoiceAnalysis.Measured
            assertEquals(frequency, result.medianHz, 1.0)
            assertTrue(result.upperHz - result.lowerHz < 1.0)
            assertTrue(result.voicedSeconds > 4.8)
        }
    }

    @Test
    fun showsPitchVariationAndLeavesMissingIntervalsAsGaps() {
        val signal = audio { time ->
            if (time in 2.0..2.4) 0.0 else 0.3 * sin(2 * PI * (140 * time + 4 * time * time))
        }
        val result = analyzeVoice(signal, rate) as VoiceAnalysis.Measured
        assertEquals(160.0, result.medianHz, 3.0)
        assertTrue(result.upperHz - result.lowerHz > 25.0)
        assertTrue(result.trace.none { it.seconds in 2.1f..2.3f })
    }

    @Test
    fun silenceClippingAndAnIncompleteCaptureReturnSpecificReasons() {
        assertEquals(VoiceAnalysis.Retry.TooQuiet, analyzeVoice(audio { 0.0 }, rate))
        assertEquals(VoiceAnalysis.Retry.Clipped, analyzeVoice(audio { 2 * sin(2 * PI * 180 * it) }, rate))
        assertEquals(VoiceAnalysis.Retry.Incomplete, analyzeVoice(ShortArray(rate), rate))
    }

    @Test
    fun rejectsBackgroundNoiseAndRecordingsWithTooLittleSustainedVoice() {
        val random = Random(83)
        val noisy = ShortArray((QUIET_SECONDS + VOICE_SECONDS) * rate) { index ->
            val noise = 0.12 * (random.nextDouble() * 2 - 1)
            val voice = if (index >= rate) 0.05 * sin(2 * PI * 160 * index / rate) else 0.0
            sample(noise + voice)
        }
        assertEquals(VoiceAnalysis.Retry.BackgroundNoise, analyzeVoice(noisy, rate))
        val stops = audio { time -> if (time % 1 < 0.65) 0.3 * sin(2 * PI * 160 * time) else 0.0 }
        assertEquals(VoiceAnalysis.Retry.NotEnoughVoice, analyzeVoice(stops, rate))
    }

    private fun audio(voice: (Double) -> Double): ShortArray =
        ShortArray((QUIET_SECONDS + VOICE_SECONDS) * rate) { index ->
            if (index < QUIET_SECONDS * rate) 0 else sample(voice(index.toDouble() / rate - QUIET_SECONDS))
        }

    private fun sample(value: Double): Short =
        (value * 32_768).toInt().coerceIn(-32_768, 32_767).toShort()
}
