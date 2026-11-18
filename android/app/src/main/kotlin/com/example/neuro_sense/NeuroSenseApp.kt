package com.example.neuro_sense

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.ceil

private val Paper = Color(0xFFF7F8F4)
private val Ink = Color(0xFF142B24)
private val Muted = Color(0xFF475D53)
private val Green = Color(0xFF175443)
private val Mint = Color(0xFFE0EDE3)
private val Line = Color(0xFFD7E0D8)
private val Card = Color(0xFFFFFFFF)

private val colors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Ink,
    secondaryContainer = Mint,
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    surfaceVariant = Mint,
    outlineVariant = Line,
)

@Composable
internal fun NeuroSenseApp(tracker: TrackerViewModel, voice: VoiceCheckViewModel, exporter: ExportViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val exportMessage = exporter.message
    LaunchedEffect(exportMessage) {
        if (exportMessage != null) {
            snackbar.showSnackbar(exportMessage)
            exporter.clearMessage()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        exporter.finish(result.data?.data.takeIf { result.resultCode == Activity.RESULT_OK })
    }
    val exportFile: (MeasurementExport) -> Unit = { export ->
        if (exporter.prepare(export)) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = export.mimeType
                putExtra(Intent.EXTRA_TITLE, export.filename)
            }
            try {
                picker.launch(intent)
            } catch (_: ActivityNotFoundException) {
                exporter.pickerUnavailable()
            } catch (_: SecurityException) {
                exporter.pickerUnavailable()
            }
        }
    }
    var voiceOpen by rememberSaveable { mutableStateOf(false) }
    var historyTab by rememberSaveable { mutableStateOf(false) }
    var chooseSetup by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    val checkIn = tracker.checkIn
    val viewedSession = tracker.viewedSession
    val comparison = tracker.comparison
    val mainScreen = checkIn is CheckIn.Ready && viewedSession == null && comparison == null && !voiceOpen
    val closeComparison = tracker::closeComparison
    val closeVoice: () -> Unit = {
        voice.reset()
        voiceOpen = false
    }
    val closeDetail: () -> Unit = {
        if (voiceOpen) closeVoice()
        else if (comparison != null) closeComparison()
        else if (viewedSession != null) tracker.closeSession()
        else if (checkIn is CheckIn.Result) tracker.closeResult()
        else tracker.cancelCapture()
    }

    BackHandler(enabled = !mainScreen || historyTab) {
        if (mainScreen) {
            tracker.closeSession()
            closeComparison()
            historyTab = false
        } else closeDetail()
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Paper,
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                    Header(
                        title = when {
                            mainScreen -> "NeuroSense"
                            voiceOpen -> "Voice check"
                            comparison != null -> "Compare checks"
                            viewedSession != null || checkIn is CheckIn.Result -> "Session"
                            else -> "Motion check"
                        },
                        back = if (mainScreen) null else closeDetail,
                        backEnabled = !tracker.editingSession && (checkIn !is CheckIn.Result || !checkIn.saving),
                    )
                    Box(Modifier.weight(1f)) {
                        when {
                            voiceOpen -> VoiceCheckScreen(voice, closeVoice, exportFile, exporter.busy)
                            comparison != null -> TraceComparison(comparison.first, comparison.second)
                            viewedSession != null -> ResultScreen(
                                session = viewedSession,
                                export = { exportFile(motionCsv(viewedSession)) },
                                exporting = exporter.busy,
                                editSetup = { chooseSetup = true },
                                delete = { deleteConfirmation = true },
                                editing = tracker.editingSession,
                                editError = tracker.editError,
                            )
                            checkIn is CheckIn.Result -> ResultScreen(
                                session = checkIn.session,
                                export = { exportFile(motionCsv(checkIn.session)) },
                                exporting = exporter.busy,
                                result = checkIn,
                                save = tracker::save,
                                retake = {
                                    tracker.closeResult()
                                    checkIn.session.summary.setup?.let(tracker::start)
                                },
                                openHistory = {
                                    tracker.closeResult()
                                    historyTab = true
                                },
                            )
                            checkIn is CheckIn.Preparing || checkIn is CheckIn.Capturing ->
                                CaptureScreen(checkIn) { tracker.cancelCapture() }
                            historyTab -> HistoryScreen(
                                history = tracker.history,
                                error = tracker.historyError,
                                retry = tracker::loadHistory,
                                openSession = {
                                    closeComparison()
                                    tracker.openSession(it)
                                },
                                compare = tracker::compareSessions,
                                loadingComparison = tracker.loadingComparison,
                                comparisonError = tracker.comparisonError,
                                start = { historyTab = false },
                            )
                            checkIn is CheckIn.Ready -> CheckInScreen(
                                latest = tracker.history?.firstOrNull(),
                                available = tracker.sensorAvailable,
                                message = checkIn.message,
                                start = { chooseSetup = true },
                                openVoice = {
                                    tracker.closeSession()
                                    voiceOpen = true
                                },
                                openSession = tracker::openSession,
                            )
                        }
                    }
                    if (mainScreen) {
                        HorizontalDivider(color = Line)
                        NavigationBar(containerColor = Paper, windowInsets = WindowInsets(0, 0, 0, 0)) {
                            NavigationBarItem(
                                selected = !historyTab,
                                onClick = {
                                    tracker.closeSession()
                                    closeComparison()
                                    historyTab = false
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Check-in") },
                            )
                            NavigationBarItem(
                                selected = historyTab,
                                onClick = {
                                    tracker.closeSession()
                                    historyTab = true
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text("History") },
                            )
                        }
                    }
                }
            }
        }
        if (chooseSetup) {
            SetupDialog(
                forCapture = viewedSession == null,
                selected = viewedSession?.summary?.setup,
                dismiss = { chooseSetup = false },
            ) { setup ->
                chooseSetup = false
                if (viewedSession == null) tracker.start(setup)
                else tracker.updateSetup(setup)
            }
        }
        if (deleteConfirmation && viewedSession != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmation = false },
                title = { Text("Delete this session?") },
                text = { Text("The recording and its measurements will be removed from History.") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteConfirmation = false
                        tracker.deleteSession()
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmation = false }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun SetupDialog(
    forCapture: Boolean,
    selected: CheckSetup?,
    dismiss: () -> Unit,
    apply: (CheckSetup) -> Unit,
) {
    var choice by rememberSaveable(selected) { mutableStateOf(selected) }
    val setup = choice
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (forCapture) "Choose your setup" else "Label this session") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (option in CheckSetup.entries) {
                    Surface(
                        onClick = { choice = option },
                        color = if (setup == option) Mint else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        ListItem(
                            headlineContent = { Text(option.label) },
                            trailingContent = if (setup == option) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else null,
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
                val hint = when (setup) {
                    CheckSetup.Table -> "Place the phone flat and avoid touching the table during capture."
                    CheckSetup.LeftHand, CheckSetup.RightHand ->
                        "Rest your forearm on a table. Keep the same grip and position each time."
                    CheckSetup.Test -> "Test recordings stay in History and are excluded from comparisons."
                    null -> "Label the setup you used so you can compare similar checks."
                }
                Text(hint, color = Muted, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { setup?.let(apply) }, enabled = setup != null) {
                Text(if (forCapture) "Begin capture" else "Save setup")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Header(title: String, back: (() -> Unit)?, backEnabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back == null) {
            Surface(shape = RoundedCornerShape(13.dp), color = Green) {
                PulseMark(Modifier.size(38.dp).padding(8.dp), Color.White)
            }
        } else {
            IconButton(onClick = back, enabled = backEnabled) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
internal fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CheckInScreen(
    latest: SessionSummary?,
    available: Boolean,
    message: String?,
    start: () -> Unit,
    openVoice: () -> Unit,
    openSession: (Long) -> Unit,
) {
    Screen {
        Heading("Your check-in", "Record movement or voice in a short, guided check.")
        Surface(color = Mint, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PulseMark(Modifier.size(32.dp), Green)
                Text("Motion check", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose a setup and use it again when you want to compare checks.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("$CAPTURE_SECONDS-second recording", color = Green, style = MaterialTheme.typography.labelLarge)
                ActionButton("Start motion check", start, enabled = available, arrow = true)
            }
        }
        val notice = message ?: if (!available) "A motion sensor is unavailable on this device." else null
        if (notice != null) Notice(notice)
        Surface(color = Card, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Voice check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Sustain an “ah” to measure your pitch and how it varies.", color = Muted)
                TextButton(onClick = openVoice) { Text("Open voice check") }
            }
        }
        Text("Last saved session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (latest == null) {
            Text("Saved checks will appear here.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        } else {
            SessionRow(latest) { openSession(latest.id) }
        }
    }
}

@Composable
private fun CaptureScreen(state: CheckIn, cancel: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val wasKeepingScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = wasKeepingScreenOn }
    }
    val capture = state as? CheckIn.Capturing
    val prep = state as? CheckIn.Preparing
    val seconds = prep?.seconds ?: ceil((1 - (capture?.progress ?: 0f)) * CAPTURE_SECONDS).toInt()
    val instruction = when (prep?.setup ?: capture?.setup) {
        CheckSetup.Table -> "Leave the phone flat and avoid touching the table."
        CheckSetup.LeftHand -> "Keep your forearm supported and hold the phone in your left hand."
        CheckSetup.RightHand -> "Keep your forearm supported and hold the phone in your right hand."
        else -> "Keep your chosen setup until the timer ends."
    }
    Screen {
        Heading(if (prep != null) "Get ready" else "Recording movement", instruction)
        Surface(color = Mint, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (prep == null) "$seconds s left" else "Starts in $seconds s",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Green,
                )
                LinearProgressIndicator(
                    progress = { capture?.progress ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                val trace = capture?.trace.orEmpty()
                if (trace.isNotEmpty()) {
                    Chart(
                        values = trace.map { it.seconds / CAPTURE_SECONDS to it.acceleration },
                        label = "Live phone movement",
                        startLabel = "0 s",
                        endLabel = "$CAPTURE_SECONDS s",
                        topValue = maxOf(0.5f, trace.maxOf { it.acceleration }),
                    )
                }
            }
        }
        TextButton(onClick = cancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel check")
        }
    }
}

@Composable
private fun ResultScreen(
    session: MotionSession,
    export: () -> Unit,
    exporting: Boolean,
    result: CheckIn.Result? = null,
    save: () -> Unit = {},
    openHistory: () -> Unit = {},
    retake: () -> Unit = {},
    editSetup: () -> Unit = {},
    delete: () -> Unit = {},
    editing: Boolean = false,
    editError: String? = null,
) {
    val summary = session.summary
    val saved = summary.id != 0L
    Screen {
        Heading(
            title = if (result == null) "Motion check" else "Check complete",
            subtitle = "${dateTime(summary.recordedAtMillis)} · ${summary.setup?.label ?: "Setup not labeled"}",
        )
        Surface(color = Mint, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Typical movement", color = Muted, style = MaterialTheme.typography.titleMedium)
                Text(
                    acceleration(summary.rms),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.SansSerif),
                    fontWeight = FontWeight.SemiBold,
                    color = Green,
                )
                Text("Summarizes how much the phone moved over $CAPTURE_SECONDS seconds. Brief spikes can raise this value.", color = Muted)
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Line)
                Text("Peak movement  ${acceleration(summary.peak)}", fontWeight = FontWeight.Medium)
                Text("${summary.sampleCount} sensor readings", color = Muted)
            }
        }
        RhythmSummary(session.rhythm)
        if (result != null && !saved && session.rhythm?.status in setOf(
                RhythmStatus.UnevenSampling, RhythmStatus.SensorLimit,
            )
        ) {
            TextButton(onClick = retake, enabled = !result.saving) { Text("Retake check") }
        }
        Text("Movement trace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SessionTrace(session, maxOf(0.1f, summary.peak), "Acceleration across the recording")
        if (result != null) {
            if (result.saveFailed) Notice("Couldn’t save this session. Try saving again.")
            if (saved) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Green)
                    Text("Saved on this device", color = Green, modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    })
                }
                ActionButton("View history", openHistory, arrow = true)
            } else {
                ActionButton(if (result.saving) "Saving…" else "Save session", save, enabled = !result.saving)
            }
        }
        TextButton(onClick = export, enabled = !exporting && !editing) {
            Text(if (session.samples.isEmpty()) "Export saved trace CSV" else "Export motion CSV")
        }
        if (result == null) {
            if (editError != null) Notice(editError)
            TextButton(onClick = editSetup, enabled = !editing) { Text("Edit setup") }
            TextButton(onClick = delete, enabled = !editing) {
                Text("Delete session", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    history: List<SessionSummary>?,
    error: Boolean,
    retry: () -> Unit,
    openSession: (Long) -> Unit,
    compare: (Long, Long) -> Unit,
    loadingComparison: Boolean,
    comparisonError: Boolean,
    start: () -> Unit,
) {
    val sessions = history.orEmpty()
    val comparable = sessions
        .filter { it.setup != null && it.setup != CheckSetup.Test }
        .groupBy { it.setup to it.source }
        .values
        .firstOrNull { it.size >= 2 }
        .orEmpty()
        .take(8)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Heading("Your history", "Compare checks recorded with the same setup.")
        }
        if (error) {
            item {
                Notice("Couldn’t open saved sessions.")
                TextButton(onClick = retry) { Text("Try again") }
            }
        }
        when {
            history == null && !error -> item { CircularProgressIndicator() }
            history.isNullOrEmpty() && !error -> item {
                Surface(color = Mint, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("No saved sessions", style = MaterialTheme.typography.titleLarge)
                        Text("Complete and save a motion check to start your history.", color = Muted)
                        ActionButton("Go to check-in", start, arrow = true)
                    }
                }
            }
        }
        if (comparable.size >= 2) {
            item {
                MovementComparison(comparable, loadingComparison) {
                    compare(comparable[0].id, comparable[1].id)
                }
                if (comparisonError) {
                    Notice("Couldn’t open the comparison. Try again.")
                }
            }
        } else if (sessions.isNotEmpty() && !error) {
            item {
                Text(
                    "Save two checks with the same setup to see a comparison. You can label older checks from their session screen.",
                    color = Muted,
                )
            }
        }
        items(history.orEmpty(), key = { it.id }) { session ->
            SessionRow(session) { openSession(session.id) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun MovementComparison(sessions: List<SessionSummary>, loading: Boolean, compare: () -> Unit) {
    val (latest, previous) = sessions
    val chronological = sessions.asReversed()
    Surface(color = Mint, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Typical movement", style = MaterialTheme.typography.titleLarge)
            Text(latest.setup?.label.orEmpty(), color = Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Latest", style = MaterialTheme.typography.labelLarge)
                    Text(acceleration(latest.rms), fontWeight = FontWeight.SemiBold)
                    Text(dateTime(latest.recordedAtMillis), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("Previous", style = MaterialTheme.typography.labelLarge)
                    Text(acceleration(previous.rms), fontWeight = FontWeight.SemiBold)
                    Text(dateTime(previous.recordedAtMillis), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (sessions.size > 2) {
                Chart(
                    values = chronological.mapIndexed { index, entry ->
                        index.toFloat() / chronological.lastIndex to entry.rms.toFloat()
                    },
                    label = "Typical movement by check, oldest to newest",
                    startLabel = "Oldest shown",
                    endLabel = "Latest",
                    topValue = maxOf(0.1f, sessions.maxOf { it.rms }.toFloat()),
                    showPoints = true,
                )
            }
            TextButton(onClick = compare, enabled = !loading) {
                Text(if (loading) "Opening traces…" else "Compare movement traces")
            }
        }
    }
}

@Composable
private fun RhythmSummary(rhythm: RhythmResult?) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Movement rhythm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (rhythm?.status == RhythmStatus.Detected) {
            val frequency = requireNotNull(rhythm.frequencyHz)
            val strength = requireNotNull(rhythm.accelerationRms)
            val formattedHz = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(frequency)
            Text("$formattedHz Hz", color = Green, style = MaterialTheme.typography.headlineSmall)
            Text("${acceleration(strength)} rhythmic acceleration (RMS)", color = Muted)
            Text("The sensor recorded a steady rhythm. This can include deliberate movement.", color = Muted)
        } else {
            Text(
                when (rhythm?.status) {
                    null -> "This check was recorded before rhythm analysis was available."
                    RhythmStatus.NoClearRhythm -> "No clear, steady rhythm in the 3–12 Hz range."
                    RhythmStatus.UnevenSampling -> "The sensor readings were too sparse or uneven to estimate a rhythm."
                    RhythmStatus.SensorLimit -> "Movement reached the sensor’s measurement limit. Retake the check for a rhythm estimate."
                    RhythmStatus.Detected -> error("Handled above")
                },
                color = Muted,
            )
        }
    }
}

@Composable
private fun TraceComparison(latest: MotionSession, previous: MotionSession) {
    val ceiling = maxOf(0.1f, latest.summary.peak, previous.summary.peak)
    Screen {
        Heading(
            "Movement traces",
            "${latest.summary.setup?.label.orEmpty()} · Both charts use the same time and acceleration scale.",
        )
        for ((label, session) in listOf("Latest check" to latest, "Previous check" to previous)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(dateTime(session.summary.recordedAtMillis), color = Muted)
                SessionTrace(session, ceiling, "$label acceleration")
            }
        }
    }
}

@Composable
private fun SessionTrace(session: MotionSession, scale: Float, label: String) {
    Chart(
        values = session.trace.map { it.seconds / CAPTURE_SECONDS to it.acceleration },
        label = label,
        startLabel = "0 s",
        endLabel = "$CAPTURE_SECONDS s",
        topValue = scale,
    )
}

@Composable
private fun SessionRow(summary: SessionSummary, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Card, shape = RoundedCornerShape(18.dp)) {
        ListItem(
            headlineContent = { Text(dateTime(summary.recordedAtMillis), fontWeight = FontWeight.Medium) },
            supportingContent = {
                Text("${acceleration(summary.rms)} · ${summary.setup?.label ?: "Setup not labeled"}", color = Muted)
            },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
internal fun ActionButton(label: String, action: () -> Unit, enabled: Boolean = true, arrow: Boolean = false) {
    Button(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Row(
            Modifier.padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (arrow) Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun Notice(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun PulseMark(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val pulse = Path().apply {
            moveTo(0f, size.height * 0.56f)
            lineTo(size.width * 0.24f, size.height * 0.56f)
            lineTo(size.width * 0.39f, size.height * 0.19f)
            lineTo(size.width * 0.58f, size.height * 0.80f)
            lineTo(size.width * 0.73f, size.height * 0.44f)
            lineTo(size.width, size.height * 0.44f)
        }
        drawPath(pulse, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun Chart(
    values: List<Pair<Float, Float>>,
    label: String,
    startLabel: String,
    endLabel: String,
    topValue: Float,
    showPoints: Boolean = false,
) {
    val ceiling = (ceil(topValue * 100) / 100).coerceAtLeast(0.01f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(acceleration(ceiling), color = Muted, style = MaterialTheme.typography.labelMedium)
        Canvas(Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = label }) {
            val inset = 3.dp.toPx()
            val width = size.width - 2 * inset
            val height = size.height - 2 * inset
            for (index in 0..3) {
                val y = inset + height * index / 3
                drawLine(Line, Offset(inset, y), Offset(inset + width, y))
            }
            val points = values.map { (fraction, value) ->
                Offset(inset + fraction * width, inset + (1 - value / ceiling) * height)
            }
            val path = Path().apply {
                points.firstOrNull()?.let { moveTo(it.x, it.y) }
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, Green, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (showPoints || points.size == 1) {
                points.forEach { drawCircle(Green, radius = inset, center = it) }
            }
        }
        Text("0", color = Muted, style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(endLabel, color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun acceleration(value: Number): String = NumberFormat.getNumberInstance().apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(value.toDouble()) + " m/s²"

private fun dateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(millis))
