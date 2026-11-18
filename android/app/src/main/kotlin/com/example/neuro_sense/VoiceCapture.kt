package com.example.neuro_sense

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

internal data class VoiceRecording(val pcm: ShortArray, val sampleRate: Int)

internal class VoiceCaptureFailure(message: String) : Exception(message)

@SuppressLint("MissingPermission") // The screen requests RECORD_AUDIO before starting.
internal suspend fun captureVoice(onProgress: suspend (samples: Int, sampleRate: Int) -> Unit): VoiceRecording {
    val (recorder, sampleRate) = openRecorder()
    val pcm = ShortArray((QUIET_SECONDS + VOICE_SECONDS) * sampleRate)
    try {
        recorder.startRecording()
        if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            throw VoiceCaptureFailure("The microphone couldn’t start. Try again.")
        }
        val deadline = SystemClock.elapsedRealtime() + (QUIET_SECONDS + VOICE_SECONDS + 3) * 1_000L
        var offset = 0
        while (offset < pcm.size) {
            currentCoroutineContext().ensureActive()
            if (SystemClock.elapsedRealtime() >= deadline) {
                throw VoiceCaptureFailure("The microphone didn’t finish recording. Try again.")
            }
            // Split at the end of the quiet baseline so the voice prompt can update immediately.
            val boundary = if (offset < QUIET_SECONDS * sampleRate) QUIET_SECONDS * sampleRate else pcm.size
            val count = recorder.read(pcm, offset, boundary - offset, AudioRecord.READ_NON_BLOCKING)
            if (count < 0) throw VoiceCaptureFailure("The microphone stopped responding. Try again.")
            if (count > 0) {
                offset += count
                onProgress(offset, sampleRate)
            }
            if (offset < pcm.size) delay(25)
        }
        return VoiceRecording(pcm, sampleRate)
    } catch (_: IllegalStateException) {
        throw VoiceCaptureFailure("The microphone became unavailable. Try again.")
    } finally {
        try {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
        } catch (_: IllegalStateException) {
            // A disconnected input may already have stopped.
        } finally {
            recorder.release()
        }
    }
}

@SuppressLint("MissingPermission")
private fun openRecorder(): Pair<AudioRecord, Int> {
    for (rate in listOf(16_000, 48_000)) {
        val minBytes = AudioRecord.getMinBufferSize(
            rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBytes <= 0) continue

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBytes, rate / 2),
            )
        } catch (_: IllegalArgumentException) {
            continue
        }
        if (recorder.state == AudioRecord.STATE_INITIALIZED) return recorder to rate
        recorder.release()
    }
    throw VoiceCaptureFailure("A compatible microphone is unavailable on this device.")
}
