package com.shonkware.droidmodloader.engine.io

import android.content.ContentResolver
import android.net.Uri
import java.io.File

internal class ArchiveImportFileStore(
    private val contentResolver: ContentResolver,
    private val externalFilesDirProvider: () -> File?,
    private val profileInternalDirProvider: () -> File,
    private val appendError: (String) -> Unit,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) {

    fun copyUriToTemporaryArchiveFile(
        uri: Uri,
        sanitizedName: String
    ): File {
        cleanOldTemporaryImportSources()

        val tempSourceDir = File(profileInternalDirProvider(), "temp/import_sources")
        tempSourceDir.mkdirs()

        val tempArchive = File(
            tempSourceDir,
            "${currentTimeMillis()}_$sanitizedName"
        )

        return copyUriToFile(uri, tempArchive)
    }

    fun copyUriToArchiveLibraryFile(
        uri: Uri,
        displayName: String
    ): File {
        val archiveLibraryDir = getArchiveLibraryDir()
            ?: throw IllegalStateException("Archive library directory is unavailable.")

        archiveLibraryDir.mkdirs()

        val destinationFile = uniqueFile(
            directory = archiveLibraryDir,
            preferredName = displayName
        )

        return copyUriToFile(uri, destinationFile)
    }

    fun cleanOldTemporaryImportSources(
        maxAgeMillis: Long = 24L * 60L * 60L * 1000L
    ) {
        val tempSourceDir = File(profileInternalDirProvider(), "temp/import_sources")
        if (!tempSourceDir.exists()) return

        val now = currentTimeMillis()

        tempSourceDir.listFiles()
            ?.filter { it.isFile }
            ?.filter { now - it.lastModified() > maxAgeMillis }
            ?.forEach { it.delete() }
    }

    private fun copyUriToFile(
        uri: Uri,
        destinationFile: File
    ): File {
        destinationFile.parentFile?.mkdirs()

        contentResolver.openInputStream(uri).use { input ->
            if (input == null) {
                throw IllegalStateException("Could not open input stream for selected file.")
            }

            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return destinationFile
    }

    private fun getArchiveLibraryDir(): File? {
        val externalBaseDir = externalFilesDirProvider()
        if (externalBaseDir == null) {
            appendError("External files directory is null")
            return null
        }

        return File(externalBaseDir, "downloads/archive_library")
    }

    private fun uniqueFile(
        directory: File,
        preferredName: String
    ): File {
        val safeName = preferredName
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .ifBlank { "imported_archive" }

        val baseName = safeName.substringBeforeLast('.', safeName)
        val extension = safeName.substringAfterLast('.', missingDelimiterValue = "")

        var candidate = File(directory, safeName)
        var index = 1

        while (candidate.exists()) {
            val nextName = if (extension.isBlank()) {
                "$baseName ($index)"
            } else {
                "$baseName ($index).$extension"
            }

            candidate = File(directory, nextName)
            index++
        }

        return candidate
    }
}
