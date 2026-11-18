package com.example.neuro_sense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var tracker: TrackerViewModel
    private lateinit var voice: VoiceCheckViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tracker = ViewModelProvider(this)[TrackerViewModel::class.java]
        voice = ViewModelProvider(this)[VoiceCheckViewModel::class.java]
        val exporter = ViewModelProvider(this)[ExportViewModel::class.java]
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        setContent { NeuroSenseApp(tracker, voice, exporter) }
    }

    override fun onStop() {
        tracker.cancelCapture(interrupted = true)
        voice.interrupt()
        super.onStop()
    }
}
