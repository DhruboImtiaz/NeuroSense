package com.example.neuro_sense

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

internal const val VOICE_SECONDS = 5
internal const val QUIET_SECONDS = 1
private const val ANALYSIS_RATE = 8_000
private const val FRAME_SIZE = ANALYSIS_RATE / 20
private const val HOP_SIZE = ANALYSIS_RATE / 40
private const val LOWEST_PITCH_HZ = 60
private const val HIGHEST_PITCH_HZ = 500

internal data class PitchPoint(val seconds: Float, val hz: Double)

internal sealed interface VoiceAnalysis {
    enum class Retry : VoiceAnalysis {
        Incomplete, Clipped, TooQuiet, BackgroundNoise, NotEnoughVoice,
    }

    data class Measured(
        val medianHz: Double,
        val lowerHz: Double,
        val upperHz: Double,
        val voicedSeconds: Double,
        val trace: List<PitchPoint>,
    ) : VoiceAnalysis
}

internal fun analyzeVoice(pcm: ShortArray, sampleRate: Int): VoiceAnalysis {
    require(sampleRate >= ANALYSIS_RATE && sampleRate % ANALYSIS_RATE == 0)
    if (pcm.size < (VOICE_SECONDS + QUIET_SECONDS) * sampleRate) return VoiceAnalysis.Retry.Incomplete

    val voiceStart = QUIET_SECONDS * sampleRate
    val end = voiceStart + VOICE_SECONDS * sampleRate
    val clipped = (voiceStart until end).count { pcm[it] <= -32_440 || pcm[it] >= 32_440 }
    if (clipped >= (end - voiceStart) / 200) return VoiceAnalysis.Retry.Clipped

    val stride = sampleRate / ANALYSIS_RATE
    val audio = DoubleArray(end / stride) { index ->
        val offset = index * stride
        (offset until offset + stride).sumOf { pcm[it].toInt() } / (stride * 32_768.0)
    }
    val voiceOffset = QUIET_SECONDS * ANALYSIS_RATE
    // Ignore the microphone's initial settling period and the transition to the prompt.
    val quietLevels = (ANALYSIS_RATE / 5 until voiceOffset * 3 / 4 step HOP_SIZE)
        .map { frameRms(audio, it) }.sorted()
    val quietLevel = percentile(quietLevels, 0.5)

    val offsets = (voiceOffset..audio.size - FRAME_SIZE step HOP_SIZE).toList()
    val levels = offsets.map { frameRms(audio, it) }
    val voiceLevel = percentile(levels.sorted(), 0.75)
    if (voiceLevel < 0.005) return VoiceAnalysis.Retry.TooQuiet
    if (quietLevel > 0.003 && voiceLevel < quietLevel * 3) return VoiceAnalysis.Retry.BackgroundNoise

    val threshold = maxOf(0.005, quietLevel * 2.5)
    val trace = offsets.mapIndexedNotNull { index, offset ->
        if (levels[index] < threshold) return@mapIndexedNotNull null
        val pitch = framePitch(audio, offset) ?: return@mapIndexedNotNull null
        val seconds = (offset - voiceOffset + FRAME_SIZE / 2).toFloat() / ANALYSIS_RATE
        PitchPoint(seconds, pitch)
    }

    val voicedSeconds = trace.size * HOP_SIZE.toDouble() / ANALYSIS_RATE
    val hasSustainedVoice = trace.windowed(2).fold(0 to 0) { (run, longest), pair ->
        val next = if (pair[1].seconds - pair[0].seconds < 0.038f) run + 1 else 0
        next to maxOf(longest, next)
    }.second >= 59
    if (voicedSeconds < 2.5 || !hasSustainedVoice) return VoiceAnalysis.Retry.NotEnoughVoice

    val sorted = trace.map { it.hz }.sorted()
    return VoiceAnalysis.Measured(
        medianHz = percentile(sorted, 0.50),
        lowerHz = percentile(sorted, 0.10),
        upperHz = percentile(sorted, 0.90),
        voicedSeconds = voicedSeconds,
        trace = trace,
    )
}

private fun frameRms(audio: DoubleArray, offset: Int): Double {
    var sum = 0.0
    var squares = 0.0
    for (index in offset until offset + FRAME_SIZE) {
        val value = audio[index]
        sum += value
        squares += value * value
    }
    val mean = sum / FRAME_SIZE
    return sqrt((squares / FRAME_SIZE - mean * mean).coerceAtLeast(0.0))
}

private fun framePitch(audio: DoubleArray, offset: Int): Double? {
    // Cumulative mean normalized difference, using the first pronounced minimum.
    val minLag = ANALYSIS_RATE / HIGHEST_PITCH_HZ
    val maxLag = ceil(ANALYSIS_RATE.toDouble() / LOWEST_PITCH_HZ).toInt()
    val size = FRAME_SIZE - maxLag - 1
    val differences = DoubleArray(maxLag + 2)
    var runningSum = 0.0
    for (lag in 1..maxLag + 1) {
        var squared = 0.0
        for (index in offset until offset + size) {
            val difference = audio[index] - audio[index + lag]
            squared += difference * difference
        }
        runningSum += squared
        differences[lag] = if (runningSum > 0) squared * lag / runningSum else 1.0
    }
    var candidate = (minLag..maxLag).firstOrNull { differences[it] < 0.18 } ?: return null
    while (candidate < maxLag && differences[candidate + 1] < differences[candidate]) candidate++
    val left = differences[candidate - 1]
    val center = differences[candidate]
    val right = differences[candidate + 1]
    val curvature = left - 2 * center + right
    val shift = if (curvature > 0) (0.5 * (left - right) / curvature).coerceIn(-0.5, 0.5) else 0.0
    return (ANALYSIS_RATE / (candidate + shift)).takeIf { it in LOWEST_PITCH_HZ.toDouble()..HIGHEST_PITCH_HZ.toDouble() }
}

private fun percentile(sorted: List<Double>, quantile: Double): Double {
    val index = quantile * sorted.lastIndex
    val lower = floor(index).toInt()
    val upper = ceil(index).toInt()
    return sorted[lower] + (index - lower) * (sorted[upper] - sorted[lower])
}
