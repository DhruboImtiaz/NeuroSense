package com.example.neuro_sense

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ExportViewModel(private val app: Application) : AndroidViewModel(app) {
    private var pending: MeasurementExport? = null

    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun prepare(export: MeasurementExport): Boolean {
        if (busy) return false
        message = null
        pending = export
        busy = true
        return true
    }

    fun pickerUnavailable() {
        pending = null
        busy = false
        message = "Couldn’t open the file picker."
    }

    fun finish(destination: Uri?) {
        val export = pending
        pending = null
        if (destination == null) {
            busy = false
            return
        }
        if (export == null) {
            busy = false
            message = "The export was interrupted. Open the result and try again."
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openOutputStream(destination, "w")
                        ?: throw IOException("The file provider did not open the destination")
                    stream.use(export.writeTo)
                }
                message = "Export saved."
            } catch (_: IOException) {
                message = "Couldn’t finish saving the export. Try again."
            } catch (_: SecurityException) {
                message = "Couldn’t write to that location. Choose a different folder."
            } catch (_: IllegalArgumentException) {
                message = "Couldn’t write to that location. Choose a different folder."
            } finally {
                busy = false
            }
        }
    }

    fun clearMessage() {
        message = null
    }
}
