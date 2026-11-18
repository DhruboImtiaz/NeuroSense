package com.example.neuro_sense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface VoiceCheck {
    data object Ready : VoiceCheck
    data class Preparing(val seconds: Int) : VoiceCheck
    data object Listening : VoiceCheck
    data class Recording(val seconds: Int, val progress: Float) : VoiceCheck
    data object Analyzing : VoiceCheck
    data class Complete(
        val result: VoiceAnalysis.Measured,
        val recordedAtMillis: Long,
        val recordingForExport: VoiceRecording?,
    ) : VoiceCheck
    data class Retry(val message: String) : VoiceCheck
}

internal class VoiceCheckViewModel : ViewModel() {
    var state by mutableStateOf<VoiceCheck>(VoiceCheck.Ready)
        private set
    private var captureJob: Job? = null

    fun start(allowAudioExport: Boolean) {
        if (captureJob?.isCompleted == false) return
        captureJob = viewModelScope.launch {
            try {
                for (seconds in 3 downTo 1) {
                    state = VoiceCheck.Preparing(seconds)
                    delay(1_000)
                }
                state = VoiceCheck.Listening
                val recording = withContext(Dispatchers.IO) {
                    captureVoice { samples, rate ->
                        val progress = (samples - QUIET_SECONDS * rate).toFloat() / (VOICE_SECONDS * rate)
                        if (progress >= 0) {
                            // Updates from capture are published through Compose's main-thread state.
                            progressUpdate(progress.coerceIn(0f, 1f))
                        }
                    }
                }
                val recordedAtMillis = System.currentTimeMillis()
                state = VoiceCheck.Analyzing
                state = when (val result = withContext(Dispatchers.Default) {
                    analyzeVoice(recording.pcm, recording.sampleRate)
                }) {
                    is VoiceAnalysis.Measured -> VoiceCheck.Complete(
                        result, recordedAtMillis, recording.takeIf { allowAudioExport },
                    )
                    is VoiceAnalysis.Retry -> VoiceCheck.Retry(result.message())
                }
            } catch (failure: VoiceCaptureFailure) {
                state = VoiceCheck.Retry(failure.message ?: "The microphone is unavailable. Try again.")
            } catch (_: SecurityException) {
                state = VoiceCheck.Retry("Microphone access is unavailable. Check the app’s microphone permission.")
            }
        }
    }

    private suspend fun progressUpdate(progress: Float) {
        withContext(Dispatchers.Main) {
            state = VoiceCheck.Recording(kotlin.math.ceil((1 - progress) * VOICE_SECONDS).toInt(), progress)
        }
    }

    fun interrupt() {
        if (state is VoiceCheck.Preparing || state is VoiceCheck.Listening || state is VoiceCheck.Recording) {
            captureJob?.cancel()
            state = VoiceCheck.Retry("The recording was interrupted. Try again when you’re ready.")
        }
    }

    fun reset() {
        captureJob?.cancel()
        state = VoiceCheck.Ready
    }
}

private fun VoiceAnalysis.Retry.message(): String = when (this) {
    VoiceAnalysis.Retry.Incomplete -> "The recording was too short. Try again."
    VoiceAnalysis.Retry.Clipped -> "The recording clipped. Move the phone a little farther away and speak at a comfortable volume."
    VoiceAnalysis.Retry.TooQuiet -> "The recording was too quiet. Check that the microphone is on and hold the phone a little closer."
    VoiceAnalysis.Retry.BackgroundNoise -> "Your voice was hard to separate from the background. Find a quieter place and stay quiet until the prompt."
    VoiceAnalysis.Retry.NotEnoughVoice -> "There wasn’t enough steady voiced sound. Use a comfortable, continuous “ah” until the timer ends."
}
