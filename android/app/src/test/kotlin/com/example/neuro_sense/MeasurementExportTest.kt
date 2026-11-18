package com.example.neuro_sense

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.ZipInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementExportTest {
    @Test
    fun motionExportPreservesSignedAxesAndRhythmMetadata() {
        val session = MotionSession(
            summary = summary,
            trace = listOf(MotionPoint(0.025f, 3f)),
            samples = listOf(MotionSample(25_000_000, MotionAxes(-1.5f, 0.5f, 2f))),
            rhythm = RhythmResult(RhythmStatus.Detected, frequencyHz = 4.75, accelerationRms = 0.25),
        )
        val export = motionCsv(session)
        val previous = Locale.getDefault()
        val rows = try {
            Locale.setDefault(Locale.FRANCE)
            export.csvRows()
        } finally {
            Locale.setDefault(previous)
        }
        assertEquals("text/csv", export.mimeType)
        assertEquals("neurosense-motion-42.csv", export.filename)
        assertEquals(2, rows.size)
        assertEquals(rows[0].size, rows[1].size)
        assertEquals(
            listOf("0.025", "-1.5", "0.5", "2.0", "axis_sample", "RightHand", "Detected", "4.75"),
            listOf(0, 1, 2, 3, 5, 7, 12, 13).map(rows[1]::get),
        )
    }

    @Test
    fun legacyMotionTraceLeavesAxisAndRhythmColumnsEmpty() {
        val session = MotionSession(summary, listOf(MotionPoint(0.2f, 2.5f)))
        val (header, row) = motionCsv(session).csvRows()
        assertEquals(header.size, row.size)
        assertEquals(listOf("", "", "", "2.5", "display_trace"), row.subList(1, 6))
        assertEquals(listOf("", "", ""), row.takeLast(3))
    }

    @Test
    fun pitchExportWritesOnlyMeasuredFramesWithTheirOriginalTimes() {
        val result = VoiceAnalysis.Measured(
            medianHz = 150.0,
            lowerHz = 145.0,
            upperHz = 155.0,
            voicedSeconds = 4.0,
            trace = listOf(PitchPoint(0.025f, 145.0), PitchPoint(1.5f, 155.0)),
        )
        val rows = pitchCsv(result, 42).csvRows()
        assertEquals(3, rows.size)
        assertEquals(listOf("1.5", "155.0", "150.0", "145.0", "155.0", "4.0"), rows.last())
    }

    @Test
    fun wavWritesMonoPcmWithoutTheQuietBaseline() {
        for (rate in listOf(16_000, 48_000)) {
            val samples = ShortArray((QUIET_SECONDS + VOICE_SECONDS) * rate) { 100 }
            samples[QUIET_SECONDS * rate] = -200
            samples[samples.lastIndex] = 300
            val data = voiceWav(VoiceRecording(samples, rate), 42).bytes()
            val reader = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", data.decodeToString(0, 4))
            assertEquals("WAVEfmt ", data.decodeToString(8, 16))
            assertEquals("data", data.decodeToString(36, 40))
            assertEquals(data.size - 8, reader.getInt(4))
            assertEquals(1, reader.getShort(20).toInt())
            assertEquals(1, reader.getShort(22).toInt())
            assertEquals(rate, reader.getInt(24))
            assertEquals(16, reader.getShort(34).toInt())
            assertEquals(rate * VOICE_SECONDS * 2, reader.getInt(40))
            assertEquals(-200, reader.getShort(44).toInt())
            assertEquals(300, reader.getShort(data.size - 2).toInt())
        }
    }

    @Test
    fun voiceBundleContainsTheMatchingStandaloneExports() {
        val result = VoiceAnalysis.Measured(150.0, 145.0, 155.0, 4.0, listOf(PitchPoint(0.025f, 150.0)))
        val recording = VoiceRecording(ShortArray(16_000 * (QUIET_SECONDS + VOICE_SECONDS)), 16_000)
        val recordedAtMillis = 1_794_581_341_720L
        val bundle = voiceValidationZip(result, recording, recordedAtMillis)
        assertEquals("application/zip", bundle.mimeType)
        assertEquals("neurosense-voice-$recordedAtMillis.zip", bundle.filename)
        val entries = ZipInputStream(ByteArrayInputStream(bundle.bytes())).use { archive ->
            buildMap {
                while (true) {
                    val entry = archive.nextEntry ?: break
                    put(entry.name, archive.readBytes())
                    archive.closeEntry()
                }
            }
        }
        val expected = listOf(pitchCsv(result, recordedAtMillis), voiceWav(recording, recordedAtMillis))
        assertEquals(expected.map { it.filename }.toSet(), entries.keys)
        for (file in expected) assertArrayEquals(file.bytes(), entries.getValue(file.filename))
    }

    private val summary = SessionSummary(
        id = 1,
        recordedAtMillis = 42,
        rms = 0.15,
        peak = 3f,
        sampleCount = 500,
        source = MotionSource.LinearAcceleration,
        setup = CheckSetup.RightHand,
    )

    private fun MeasurementExport.bytes(): ByteArray =
        ByteArrayOutputStream().also(writeTo).toByteArray()

    private fun MeasurementExport.csvRows(): List<List<String>> =
        bytes().decodeToString().trimEnd('\r', '\n').lines().map { it.split(",") }
}
