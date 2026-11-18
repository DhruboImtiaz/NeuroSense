package com.example.neuro_sense

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class MeasurementExport(
    val filename: String,
    val mimeType: String,
    val writeTo: (OutputStream) -> Unit,
)

internal fun motionCsv(session: MotionSession): MeasurementExport =
    MeasurementExport("neurosense-motion-${session.summary.recordedAtMillis}.csv", "text/csv") { stream ->
        val summary = session.summary
        val rhythm = session.rhythm
        val writer = stream.bufferedWriter(Charsets.UTF_8)
        writer.appendLine(
            "elapsed_s,accel_x_m_s2,accel_y_m_s2,accel_z_m_s2,accel_magnitude_m_s2," +
                "data_kind,recorded_at_epoch_ms,setup,sensor_method,sensor_sample_count," +
                "movement_rms_m_s2,peak_m_s2,rhythm_status,rhythm_hz,rhythmic_rms_m_s2",
        )
        val metadata = listOf(
            summary.recordedAtMillis, summary.setup?.name, summary.source.name, summary.sampleCount,
            summary.rms, summary.peak, rhythm?.status?.name, rhythm?.frequencyHz, rhythm?.accelerationRms,
        ).joinToString(",") { it?.toString().orEmpty() }

        if (session.samples.isEmpty()) {
            for (point in session.trace) {
                writer.appendLine("${point.seconds},,,,${point.acceleration},display_trace,$metadata")
            }
        } else {
            for ((elapsedNanos, axes) in session.samples) {
                writer.appendLine(
                    "${elapsedNanos / 1e9},${axes.x},${axes.y},${axes.z},${axes.magnitude},axis_sample,$metadata",
                )
            }
        }
        writer.flush()
    }

internal fun pitchCsv(result: VoiceAnalysis.Measured, recordedAtMillis: Long): MeasurementExport =
    MeasurementExport("neurosense-voice-$recordedAtMillis.csv", "text/csv") { stream ->
        val writer = stream.bufferedWriter(Charsets.UTF_8)
        writer.appendLine(
            "elapsed_s,pitch_hz,median_hz,percentile_10_hz,percentile_90_hz,voiced_seconds",
        )
        val metadata = listOf(result.medianHz, result.lowerHz, result.upperHz, result.voicedSeconds).joinToString(",")
        for ((seconds, pitch) in result.trace) writer.appendLine("$seconds,$pitch,$metadata")
        writer.flush()
    }

internal fun voiceWav(recording: VoiceRecording, recordedAtMillis: Long): MeasurementExport =
    MeasurementExport("neurosense-voice-$recordedAtMillis.wav", "audio/wav") { stream ->
        val rate = recording.sampleRate
        val firstSample = QUIET_SECONDS * rate
        val bytesPerSample = Short.SIZE_BYTES
        val sampleCount = recording.pcm.size - firstSample
        val dataSize = sampleCount * bytesPerSample
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + dataSize)
        header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
        header.putShort(1).putShort(1) // PCM, one channel.
        header.putInt(rate).putInt(rate * bytesPerSample)
        header.putShort(bytesPerSample.toShort()).putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII)).putInt(dataSize)
        stream.write(header.array())

        val block = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        for (index in firstSample until recording.pcm.size) {
            block.putShort(recording.pcm[index])
            if (!block.hasRemaining()) {
                stream.write(block.array())
                block.clear()
            }
        }
        stream.write(block.array(), 0, block.position())
    }

internal fun voiceValidationZip(
    result: VoiceAnalysis.Measured,
    recording: VoiceRecording,
    recordedAtMillis: Long,
): MeasurementExport =
    MeasurementExport("neurosense-voice-$recordedAtMillis.zip", "application/zip") { stream ->
        ZipOutputStream(stream).use { archive ->
            for (export in listOf(pitchCsv(result, recordedAtMillis), voiceWav(recording, recordedAtMillis))) {
                archive.putNextEntry(ZipEntry(export.filename).apply { time = recordedAtMillis })
                export.writeTo(archive)
                archive.closeEntry()
            }
        }
    }
