package com.example.neuro_sense

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class RhythmStatus {
    Detected,
    NoClearRhythm,
    UnevenSampling,
    SensorLimit,
}

internal data class RhythmResult(
    val status: RhythmStatus,
    val frequencyHz: Double? = null,
    val accelerationRms: Double? = null,
)

private const val MIN_FREQUENCY_HZ = 3.0
private const val MAX_FREQUENCY_HZ = 12.0
private const val SAMPLE_INTERVAL_NANOS = 20_000_000L
private const val MAX_RHYTHM_GAP_NANOS = 70_000_000L
private const val WINDOW_SIZE = 240
private const val FREQUENCY_STEP_HZ = 50.0 / WINDOW_SIZE

internal fun estimateRhythm(samples: List<MotionSample>, clipped: Boolean): RhythmResult {
    if (clipped) return RhythmResult(RhythmStatus.SensorLimit)

    val grid = evenlySpaced(samples) ?: return RhythmResult(RhythmStatus.UnevenSampling)
    val first = spectralPeak(grid.take(WINDOW_SIZE))
    val second = spectralPeak(grid.takeLast(WINDOW_SIZE))
    if (first == null || second == null || abs(first.hz - second.hz) > 0.5 ||
        minOf(first.rms, second.rms) < maxOf(first.rms, second.rms) * 0.25
    ) return RhythmResult(RhythmStatus.NoClearRhythm)

    return RhythmResult(
        status = RhythmStatus.Detected,
        frequencyHz = (first.hz + second.hz) / 2,
        accelerationRms = sqrt((first.rms * first.rms + second.rms * second.rms) / 2),
    )
}

private fun evenlySpaced(samples: List<MotionSample>): List<MotionAxes>? {
    // Leave 200 ms at each edge and require coverage without extrapolation.
    val start = 200_000_000L
    val end = start + (2 * WINDOW_SIZE - 1) * SAMPLE_INTERVAL_NANOS
    if (samples.size < 2 || samples.first().offsetNanos > start || samples.last().offsetNanos < end) return null
    val sampleSpan = samples.last().offsetNanos - samples.first().offsetNanos
    if ((samples.size - 1) * 1e9 / sampleSpan < 40 ||
        samples.zipWithNext().any { (a, b) -> b.offsetNanos - a.offsetNanos !in 1..MAX_RHYTHM_GAP_NANOS }
    ) return null

    var index = 0
    return List(2 * WINDOW_SIZE) { step ->
        val time = start + step * SAMPLE_INTERVAL_NANOS
        while (samples[index + 1].offsetNanos < time) index++
        val a = samples[index]
        val b = samples[index + 1]
        val fraction = (time - a.offsetNanos).toFloat() / (b.offsetNanos - a.offsetNanos)
        MotionAxes(
            a.axes.x + fraction * (b.axes.x - a.axes.x),
            a.axes.y + fraction * (b.axes.y - a.axes.y),
            a.axes.z + fraction * (b.axes.z - a.axes.z),
        )
    }
}

private data class SpectralPeak(val hz: Double, val rms: Double)

private fun spectralPeak(window: List<MotionAxes>): SpectralPeak? {
    val length = window.size
    val meanX = window.sumOf { it.x.toDouble() } / length
    val meanY = window.sumOf { it.y.toDouble() } / length
    val meanZ = window.sumOf { it.z.toDouble() } / length
    val weights = DoubleArray(length) { 0.5 - 0.5 * cos(2 * PI * it / (length - 1)) }
    val x = DoubleArray(length) { (window[it].x - meanX) * weights[it] }
    val y = DoubleArray(length) { (window[it].y - meanY) * weights[it] }
    val z = DoubleArray(length) { (window[it].z - meanZ) * weights[it] }
    val lastBin = (18 / FREQUENCY_STEP_HZ).toInt()
    val power = DoubleArray(lastBin + 1)
    for (bin in 1..lastBin) {
        var realX = 0.0
        var realY = 0.0
        var realZ = 0.0
        var imagX = 0.0
        var imagY = 0.0
        var imagZ = 0.0
        for (i in window.indices) {
            val angle = 2 * PI * bin * i / length
            val real = cos(angle)
            val imag = sin(angle)
            realX += x[i] * real
            realY += y[i] * real
            realZ += z[i] * real
            imagX += x[i] * imag
            imagY += y[i] * imag
            imagZ += z[i] * imag
        }
        power[bin] = realX * realX + realY * realY + realZ * realZ +
            imagX * imagX + imagY * imagY + imagZ * imagZ
    }

    val peak = (1..lastBin).maxBy { power[it] }
    if (peak <= 1 || peak >= lastBin || power[peak] <= 0) return null
    val localPower = power[peak - 1] + power[peak] + power[peak + 1]
    val rms = sqrt(2 * localPower / (length * weights.sumOf { it * it }))
    // These are engineering gates for a repeatable peak, not clinical cutoffs.
    if (rms < 0.03 || localPower / power.sum() < 0.5) return null

    val left = power[peak - 1]
    val center = power[peak]
    val right = power[peak + 1]
    val curvature = left - 2 * center + right
    val shift = if (curvature < 0) (0.5 * (left - right) / curvature).coerceIn(-0.5, 0.5) else 0.0
    val hz = (peak + shift) * FREQUENCY_STEP_HZ
    if (hz !in MIN_FREQUENCY_HZ..MAX_FREQUENCY_HZ) return null
    return SpectralPeak(hz, rms)
}
