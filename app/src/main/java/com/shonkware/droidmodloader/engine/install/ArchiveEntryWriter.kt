package com.shonkware.droidmodloader.engine.install

import android.util.Log
import java.io.File
import java.io.OutputStream

class ArchiveEntryWriter(
    private val outputDir: File
) {
    companion object {
        private const val TAG = "DroidModLoader"
    }

    fun writeDirectory(rawEntryName: String) {
        val outFile = resolveArchiveEntryOrNull(rawEntryName) ?: return
        outFile.mkdirs()
    }

    fun writeFile(
        rawEntryName: String,
        writeContent: (OutputStream) -> Unit
    ) {
        val outFile = resolveArchiveEntryOrNull(rawEntryName) ?: return

        outFile.parentFile?.mkdirs()

        outFile.outputStream().use { output ->
            writeContent(output)
        }
    }

    private fun resolveArchiveEntryOrNull(rawEntryName: String): File? {
        return when (val normalized = ArchiveEntryPath.normalize(rawEntryName)) {
            ArchiveEntryPathResult.Ignore -> {
                Log.d(TAG, "Ignored archive metadata/root entry: $rawEntryName")
                null
            }

            is ArchiveEntryPathResult.Valid -> {
                ArchiveEntryPath.safeResolve(outputDir, normalized.relativePath)
            }
        }
    }
}