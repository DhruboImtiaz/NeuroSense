package com.example.neuro_sense

import android.app.Application
import android.database.sqlite.SQLiteException
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface CheckIn {
    data class Ready(val message: String? = null) : CheckIn
    data class Preparing(val seconds: Int, val setup: CheckSetup) : CheckIn
    data class Capturing(val progress: Float, val trace: List<MotionPoint>, val setup: CheckSetup) : CheckIn
    data class Result(
        val session: MotionSession,
        val saving: Boolean = false,
        val saveFailed: Boolean = false,
    ) : CheckIn
}

internal class TrackerViewModel(private val app: Application) : AndroidViewModel(app) {
    private val capture = MotionCapture(app)
    private var captureJob: Job? = null
    private var historyJob: Job? = null
    private var detailJob: Job? = null
    private var comparisonJob: Job? = null

    var editingSession by mutableStateOf(false)
        private set
    var editError by mutableStateOf<String?>(null)
        private set

    var comparison by mutableStateOf<Pair<MotionSession, MotionSession>?>(null)
        private set
    var loadingComparison by mutableStateOf(false)
        private set
    var comparisonError by mutableStateOf(false)
        private set

    val sensorAvailable get() = capture.available

    var checkIn by mutableStateOf<CheckIn>(CheckIn.Ready())
        private set
    var history by mutableStateOf<List<SessionSummary>?>(null)
        private set
    var historyError by mutableStateOf(false)
        private set
    var viewedSession by mutableStateOf<MotionSession?>(null)
        private set

    init {
        loadHistory()
    }

    fun start(setup: CheckSetup) {
        if (checkIn !is CheckIn.Ready) return
        closeSession()

        val startNanos = SystemClock.elapsedRealtimeNanos() + PREP_SECONDS * 1_000_000_000L
        val endNanos = startNanos + CAPTURE_NANOS
        val window = MotionWindow(startNanos)
        val source = capture.start(window::add)
        if (source == null) {
            checkIn = CheckIn.Ready("The motion sensor is unavailable. Try again on a device with an accelerometer.")
            return
        }
        val recordedAt = System.currentTimeMillis() + PREP_SECONDS * 1_000L
        checkIn = CheckIn.Preparing(PREP_SECONDS, setup)
        captureJob = viewModelScope.launch {
            while (true) {
                val now = SystemClock.elapsedRealtimeNanos()
                if (now >= endNanos) break
                checkIn = if (now < startNanos) {
                    val seconds = ((startNanos - now + 999_999_999L) / 1_000_000_000L).toInt()
                    CheckIn.Preparing(seconds, setup)
                } else {
                    val progress = (now - startNanos).toFloat() / CAPTURE_NANOS
                    CheckIn.Capturing(progress, window.trace(), setup)
                }
                delay(100)
            }
            capture.stop()
            val reading = window.finish()
            checkIn = if (reading == null) {
                CheckIn.Ready("The sensor didn’t send enough continuous data. Try another check.")
            } else {
                val rhythm = withContext(Dispatchers.Default) {
                    estimateRhythm(reading.samples, reading.clipped)
                }
                CheckIn.Result(
                    MotionSession(
                        summary = SessionSummary(
                            0, recordedAt, reading.rms, reading.peak, reading.sampleCount, source, setup,
                        ),
                        trace = reading.trace,
                        samples = reading.samples,
                        rhythm = rhythm,
                    ),
                )
            }
        }
    }

    fun cancelCapture(interrupted: Boolean = false) {
        if (checkIn !is CheckIn.Preparing && checkIn !is CheckIn.Capturing) return

        captureJob?.cancel()
        capture.stop()
        checkIn = CheckIn.Ready(if (interrupted) "Capture was interrupted. Start a new check when you’re ready." else null)
    }

    fun save() {
        val result = checkIn as? CheckIn.Result ?: return
        if (result.saving || result.session.summary.id != 0L) return

        checkIn = result.copy(saving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    SessionStore(app).use { it.save(result.session) }
                }
                checkIn = CheckIn.Result(saved)
                loadHistory()
            } catch (_: SQLiteException) {
                checkIn = result.copy(saveFailed = true)
            }
        }
    }

    fun closeResult() {
        val result = checkIn as? CheckIn.Result ?: return
        if (!result.saving) checkIn = CheckIn.Ready()
    }

    fun loadHistory() {
        historyError = false
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            try {
                history = withContext(Dispatchers.IO) {
                    SessionStore(app).use { it.summaries() }
                }
            } catch (_: SQLiteException) {
                historyError = true
            }
        }
    }

    fun compareSessions(latest: Long, previous: Long) {
        if (loadingComparison) return
        comparisonError = false
        loadingComparison = true
        comparisonJob = viewModelScope.launch {
            try {
                comparison = withContext(Dispatchers.IO) {
                    SessionStore(app).use { store ->
                        val first = store.session(latest) ?: return@use null
                        val second = store.session(previous) ?: return@use null
                        first to second
                    }
                }
                comparisonError = comparison == null
            } catch (_: SQLiteException) {
                comparisonError = true
            } finally {
                loadingComparison = false
            }
        }
    }

    fun closeComparison() {
        comparisonJob?.cancel()
        comparison = null
        loadingComparison = false
        comparisonError = false
    }

    fun openSession(id: Long) {
        if (editingSession) return
        editError = null
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            try {
                viewedSession = withContext(Dispatchers.IO) {
                    SessionStore(app).use { it.session(id) }
                }
                if (viewedSession == null) historyError = true
            } catch (_: SQLiteException) {
                historyError = true
            }
        }
    }

    fun closeSession() {
        if (editingSession) return
        editError = null
        detailJob?.cancel()
        viewedSession = null
    }

    fun updateSetup(setup: CheckSetup) {
        val session = viewedSession ?: return
        if (session.summary.setup == setup) return
        editSession("Couldn’t save the setup. Try again.") { store ->
            store.updateSetup(session.summary.id, setup)
            session.copy(summary = session.summary.copy(setup = setup))
        }
    }

    fun deleteSession() {
        val session = viewedSession ?: return
        editSession("Couldn’t delete the session. Try again.") { store ->
            store.delete(session.summary.id)
            null
        }
    }

    private fun editSession(error: String, edit: (SessionStore) -> MotionSession?) {
        if (editingSession) return
        editingSession = true
        editError = null
        viewModelScope.launch {
            try {
                viewedSession = withContext(Dispatchers.IO) {
                    SessionStore(app).use(edit)
                }
                loadHistory()
            } catch (_: SQLiteException) {
                editError = error
            } finally {
                editingSession = false
            }
        }
    }

    override fun onCleared() {
        capture.stop()
        super.onCleared()
    }
}
