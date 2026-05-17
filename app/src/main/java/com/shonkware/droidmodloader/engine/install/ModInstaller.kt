package com.shonkware.droidmodloader.engine.install

import android.util.Log
import java.io.File
import java.io.IOException

class ModInstaller(
    private val tempDir: File,
    private val modsDir: File,
    private val archiveExtractor: ArchiveExtractor = ArchiveExtractor()
) {

    companion object {
        private const val TAG = "DroidModLoader"
    }

    fun installArchive(archive: File): File {
        Log.d(TAG, "ModInstaller.installArchive start: ${archive.absolutePath}")
        Log.d(TAG, "Archive exists=${archive.exists()} size=${archive.length()} extension=${archive.extension}")

        val extractFolder = File(tempDir, "direct_install_${System.currentTimeMillis()}")
        val rawRoot = File(extractFolder, "raw")
        val modName = archive.nameWithoutExtension
        val finalDir = File(modsDir, modName)

        try {
            if (!rawRoot.mkdirs()) {
                throw IOException("Could not create raw extract folder: ${rawRoot.absolutePath}")
            }

            archiveExtractor.extractToRawFolder(archive, rawRoot)

            val normalizedRoot = normalizeExtractedStructure(rawRoot)

            if (finalDir.exists()) {
                finalDir.deleteRecursively()
            }

            if (normalizedRoot.renameTo(finalDir)) {
                return finalDir
            }

            normalizedRoot.copyRecursively(finalDir, overwrite = true)
            return finalDir
        } catch (t: Throwable) {
            if (finalDir.exists()) {
                finalDir.deleteRecursively()
            }

            throw IOException(
                "Archive install failed for ${archive.name}: ${t.message}",
                t
            )
        } finally {
            if (extractFolder.exists()) {
                extractFolder.deleteRecursively()
            }
        }
    }

    private fun normalizeExtractedStructure(root: File): File {
        val children = root.listFiles() ?: return root

        if (children.size == 1 && children[0].isDirectory) {
            val child = children[0]
            val dataFolder = File(child, "Data")
            if (dataFolder.exists()) {
                return dataFolder
            }
            return child
        }

        val dataFolder = File(root, "Data")
        if (dataFolder.exists()) {
            return dataFolder
        }

        return root
    }
}