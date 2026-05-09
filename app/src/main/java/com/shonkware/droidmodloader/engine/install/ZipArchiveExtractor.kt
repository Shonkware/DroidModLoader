package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.util.zip.ZipInputStream

class ZipArchiveExtractor : ArchiveExtractor {

    override fun supports(archive: File): Boolean {
        return archive.extension.lowercase() == "zip"
    }

    override fun extract(archive: File, tempDir: File, modsDir: File): File {
        val extractFolder = File(tempDir, System.currentTimeMillis().toString())
        extractFolder.mkdirs()

        extractZip(archive, extractFolder)

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

    private fun extractZip(zipFile: File, outputDir: File) {
        val buffer = ByteArray(1024 * 8)

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry

            while (entry != null) {
                val newFile = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    newFile.outputStream().use { fos ->
                        var len = zis.read(buffer)
                        while (len > 0) {
                            fos.write(buffer, 0, len)
                            len = zis.read(buffer)
                        }
                    }
                }

                entry = zis.nextEntry
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