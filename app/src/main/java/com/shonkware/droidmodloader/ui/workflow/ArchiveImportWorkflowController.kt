package com.shonkware.droidmodloader.ui.workflow

import android.net.Uri

internal class ArchiveImportWorkflowController(
    private val appendLog: (String) -> Unit,
    private val runInBackground: (() -> Unit) -> Unit,
    private val handleImportedArchive: (Uri) -> Unit,
    private val showArchiveLibrarySummary: () -> Unit
) {

    fun handleArchivePickerResult(uri: Uri?) {
        if (uri == null) {
            appendLog("No file selected.")
            return
        }

        runInBackground {
            handleImportedArchive(uri)
        }
    }

    fun requestArchiveLibrarySummary() {
        runInBackground {
            showArchiveLibrarySummary()
        }
    }
}