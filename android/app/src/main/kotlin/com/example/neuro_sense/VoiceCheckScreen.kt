package com.example.neuro_sense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.floor

@Composable
internal fun VoiceCheckScreen(
    voice: VoiceCheckViewModel,
    close: () -> Unit,
    export: (MeasurementExport) -> Unit,
    exporting: Boolean,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val state = voice.state
    val recording = state is VoiceCheck.Preparing || state is VoiceCheck.Listening || state is VoiceCheck.Recording
    val microphoneAvailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    var allowAudioExport by rememberSaveable { mutableStateOf(false) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) voice.start(allowAudioExport)
    }
    val start: () -> Unit = {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            denied = false
            voice.start(allowAudioExport)
        } else {
            request.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (recording) {
        DisposableEffect(view) {
            val previous = view.keepScreenOn
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = previous }
        }
    }

    Screen {
        when (state) {
            VoiceCheck.Ready, is VoiceCheck.Retry -> {
                Heading("Voice check", "Measure how your pitch changes while you sustain an “ah”.")
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Before you start", style = MaterialTheme.typography.titleMedium)
                        Text("Find a quiet place. Hold the phone about a hand’s length from your mouth.")
                        Text("Stay quiet until the prompt, then say “ah” for $VOICE_SECONDS seconds at a comfortable pitch and volume.")
                    }
                }
                if (!microphoneAvailable) {
                    Notice("This device has no available microphone.")
                }
                if (state is VoiceCheck.Retry) Notice(state.message)
                if (denied) Notice("Allow microphone access to run a voice check.")
                AudioExportOption(allowAudioExport) { allowAudioExport = it }
                ActionButton(
                    if (state is VoiceCheck.Retry || denied) "Try again" else "Start voice check",
                    start,
                    enabled = microphoneAvailable,
                )
                if (denied) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                    }) { Text("Open app settings") }
                }
            }
            is VoiceCheck.Preparing -> {
                Heading("Get ready", "Hold your phone in position and stay quiet.")
                Text("Starts in ${state.seconds} s", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = close) { Text("Cancel check") }
            }
            VoiceCheck.Listening -> {
                Heading("Stay quiet", "Measuring the background sound.")
                CircularProgressIndicator()
                TextButton(onClick = close) { Text("Cancel check") }
            }
            is VoiceCheck.Recording -> {
                Text(
                    "Say “ah” now",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text("Keep a comfortable pitch and volume.")
                Text("${state.seconds.coerceAtLeast(1)} s left", style = MaterialTheme.typography.headlineMedium)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = close) { Text("Cancel check") }
            }
            VoiceCheck.Analyzing -> {
                Heading("Measuring pitch", "Checking the recording for usable voiced sound.")
                CircularProgressIndicator()
            }
            is VoiceCheck.Complete -> {
                Heading("Voice check complete", "Pitch measured during your sustained “ah”.")
                VoiceResult(state.result)
                val recording = state.recordingForExport
                if (recording != null) {
                    ActionButton(
                        "Export audio and pitch (ZIP)",
                        { export(voiceValidationZip(state.result, recording, state.recordedAtMillis)) },
                        enabled = !exporting,
                    )
                }
                ActionButton(
                    "Record again",
                    {
                        allowAudioExport = false
                        voice.reset()
                    },
                    enabled = !exporting,
                )
                TextButton(
                    onClick = { export(pitchCsv(state.result, state.recordedAtMillis)) },
                    enabled = !exporting,
                ) { Text("Export pitch CSV") }
                if (recording != null) {
                    TextButton(
                        onClick = { export(voiceWav(recording, state.recordedAtMillis)) },
                        enabled = !exporting,
                    ) { Text("Export audio WAV") }
                }
            }
        }
    }
}

@Composable
private fun AudioExportOption(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Enable audio export for this check")
            Text(
                "The recording stays in memory until you leave or start over. A copy is saved only when you export audio.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun VoiceResult(result: VoiceAnalysis.Measured) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Median pitch", style = MaterialTheme.typography.titleMedium)
            Text("${formatVoiceNumber(result.medianHz)} Hz", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text("Pitch variation  ${formatVoiceNumber(result.upperHz - result.lowerHz)} Hz")
            Text(
                "Middle 80% of estimates  ${formatVoiceNumber(result.lowerHz)}–${formatVoiceNumber(result.upperHz)} Hz",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("${formatVoiceNumber(result.voicedSeconds)} s of usable voiced audio", style = MaterialTheme.typography.bodySmall)
        }
    }
    Text("Pitch over time", style = MaterialTheme.typography.titleMedium)
    PitchChart(result.trace)
}

@Composable
private fun PitchChart(trace: List<PitchPoint>) {
    val min = floor(trace.minOf { it.hz } / 10) * 10
    val max = maxOf(min + 10, ceil(trace.maxOf { it.hz } / 10) * 10)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${formatVoiceNumber(max)} Hz", style = MaterialTheme.typography.labelMedium)
        Canvas(Modifier.fillMaxWidth().height(112.dp).semantics {
            contentDescription = "Voice pitch over five seconds. Gaps show sections without a usable pitch estimate."
        }) {
            val inset = 3.dp.toPx()
            val width = size.width - inset * 2
            val height = size.height - inset * 2
            for (index in 0..3) {
                val y = inset + height * index / 3
                drawLine(grid, Offset(inset, y), Offset(inset + width, y))
            }
            val path = Path()
            var previousTime = -1f
            for (point in trace) {
                val x = inset + point.seconds / VOICE_SECONDS * width
                val y = inset + (1 - (point.hz - min) / (max - min)).toFloat() * height
                if (point.seconds - previousTime > 0.038f) path.moveTo(x, y) else path.lineTo(x, y)
                previousTime = point.seconds
            }
            drawPath(path, ink, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("${formatVoiceNumber(min)} Hz", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0 s", style = MaterialTheme.typography.labelMedium)
            Text("$VOICE_SECONDS s", style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatVoiceNumber(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 1
}.format(value)
