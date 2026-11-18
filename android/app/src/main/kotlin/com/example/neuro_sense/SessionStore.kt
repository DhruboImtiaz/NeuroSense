package com.example.neuro_sense

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.nio.ByteBuffer
import org.json.JSONArray

internal enum class CheckSetup(val label: String) {
    Table("On a table"),
    LeftHand("Left hand"),
    RightHand("Right hand"),
    Test("Test recording"),
}

internal data class SessionSummary(
    val id: Long,
    val recordedAtMillis: Long,
    val rms: Double,
    val peak: Float,
    val sampleCount: Int,
    val source: MotionSource,
    val setup: CheckSetup? = null,
)

internal data class MotionSession(
    val summary: SessionSummary,
    val trace: List<MotionPoint>,
    val samples: List<MotionSample> = emptyList(),
    val rhythm: RhythmResult? = null,
)

internal class SessionStore(context: Context) :
    SQLiteOpenHelper(context, "motion_sessions.db", null, 3) {
    private val summaryColumns = arrayOf("id", "recorded_at", "rms", "peak", "sample_count", "source", "setup")

    private val detailColumns = summaryColumns + arrayOf(
        "trace", "samples", "rhythm_status", "rhythm_hz", "rhythm_rms",
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY,
                recorded_at INTEGER NOT NULL,
                rms REAL NOT NULL,
                peak REAL NOT NULL,
                sample_count INTEGER NOT NULL,
                source TEXT NOT NULL,
                setup TEXT,
                trace TEXT NOT NULL,
                samples BLOB,
                rhythm_status TEXT,
                rhythm_hz REAL,
                rhythm_rms REAL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE sessions ADD COLUMN setup TEXT")
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN samples BLOB")
            db.execSQL("ALTER TABLE sessions ADD COLUMN rhythm_status TEXT")
            db.execSQL("ALTER TABLE sessions ADD COLUMN rhythm_hz REAL")
            db.execSQL("ALTER TABLE sessions ADD COLUMN rhythm_rms REAL")
        }
    }

    fun updateSetup(id: Long, setup: CheckSetup) {
        val values = ContentValues().apply { put("setup", setup.name) }
        writableDatabase.update("sessions", values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("sessions", "id = ?", arrayOf(id.toString()))
    }

    fun save(session: MotionSession): MotionSession {
        val summary = session.summary
        val values = ContentValues().apply {
            put("recorded_at", summary.recordedAtMillis)
            put("rms", summary.rms)
            put("peak", summary.peak)
            put("sample_count", summary.sampleCount)
            put("source", summary.source.name)
            put("setup", summary.setup?.name)
            put("samples", encodeSamples(session.samples))
            put("rhythm_status", session.rhythm?.status?.name)
            put("rhythm_hz", session.rhythm?.frequencyHz)
            put("rhythm_rms", session.rhythm?.accelerationRms)
            put("trace", JSONArray().apply {
                session.trace.forEach { point ->
                    put(JSONArray().put(point.seconds.toDouble()).put(point.acceleration.toDouble()))
                }
            }.toString())
        }
        val id = writableDatabase.insertOrThrow("sessions", null, values)
        return session.copy(summary = summary.copy(id = id))
    }

    fun summaries(): List<SessionSummary> =
        readableDatabase.query(
            "sessions", summaryColumns, null, null, null, null, "recorded_at DESC, id DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.summary())
            }
        }

    fun session(id: Long): MotionSession? =
        readableDatabase.query(
            "sessions", detailColumns, "id = ?", arrayOf(id.toString()), null, null, null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null

            val points = JSONArray(cursor.getString(summaryColumns.size))
            val trace = List(points.length()) { index ->
                val point = points.getJSONArray(index)
                MotionPoint(point.getDouble(0).toFloat(), point.getDouble(1).toFloat())
            }
            val statusColumn = cursor.getColumnIndexOrThrow("rhythm_status")
            val status = RhythmStatus.entries.firstOrNull { it.name == cursor.getString(statusColumn) }
            val rhythm = status?.let {
                RhythmResult(it, cursor.optionalDouble("rhythm_hz"), cursor.optionalDouble("rhythm_rms"))
            }
            val sampleBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("samples"))
            MotionSession(cursor.summary(), trace, decodeSamples(sampleBytes), rhythm)
        }

    private fun Cursor.summary() = SessionSummary(
        id = getLong(0),
        recordedAtMillis = getLong(1),
        rms = getDouble(2),
        peak = getFloat(3),
        sampleCount = getInt(4),
        source = MotionSource.valueOf(getString(5)),
        setup = CheckSetup.entries.firstOrNull { it.name == getString(6) },
    )
}

private const val SAMPLE_BYTES = Long.SIZE_BYTES + 3 * Float.SIZE_BYTES

private fun encodeSamples(samples: List<MotionSample>): ByteArray =
    ByteBuffer.allocate(samples.size * SAMPLE_BYTES).apply {
        for (sample in samples) {
            putLong(sample.offsetNanos)
            putFloat(sample.axes.x)
            putFloat(sample.axes.y)
            putFloat(sample.axes.z)
        }
    }.array()

private fun decodeSamples(bytes: ByteArray?): List<MotionSample> {
    if (bytes == null) return emptyList()
    val buffer = ByteBuffer.wrap(bytes)
    return List(bytes.size / SAMPLE_BYTES) {
        MotionSample(buffer.getLong(), MotionAxes(buffer.getFloat(), buffer.getFloat(), buffer.getFloat()))
    }
}

private fun Cursor.optionalDouble(column: String): Double? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getDouble(index)
}
