package com.shonkware.droidmodloader.engine.install

import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File

class SevenZipArchiveExtractor : ArchiveExtractor {

    override fun supports(archive: File): Boolean {
        return archive.extension.lowercase() == "7z"
    }

    override fun extract(archive: File, tempDir: File, modsDir: File): File {
        val extractFolder = File(tempDir, System.currentTimeMillis().toString())
        extractFolder.mkdirs()

        extractSevenZip(archive, extractFolder)

        val normalizedRoot = normalizeExtractedStructure(extractFolder)
        val modName = archive.nameWithoutExtension
        val finalDir = File(modsDir, modName)

        if (finalDir.exists()) {
            finalDir.deleteRecursively()
        }

        normalizedRoot.copyRecursively(finalDir, overwrite = true)
        extractFolder.deleteRecursively()

        return finalDir
    }

    private fun extractSevenZip(archiveFile: File, outputDir: File) {
        SevenZFile(archiveFile).use { sevenZ ->
            var entry = sevenZ.nextEntry

            while (entry != null) {
                val outFile = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { fos ->
                        val buffer = ByteArray(1024 * 8)
                        var remaining = entry.size

                        while (remaining > 0) {
                            val read = sevenZ.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read <= 0) break
                            fos.write(buffer, 0, read)
                            remaining -= read
                        }
                    }
                }

                entry = sevenZ.nextEntry
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