package com.shonkware.droidmodloader.engine.install

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import java.io.File
import java.io.IOException

class RarArchiveReader : ArchiveReader {

    override fun supports(archive: File): Boolean {
        return archive.extension.lowercase() == "rar"
    }

    override fun read(
        archive: File,
        writer: ArchiveEntryWriter
    ) {
        try {
            Archive(archive).use { rar ->
                while (true) {
                    val header: FileHeader = rar.nextFileHeader() ?: break
                    val rawName = getRarEntryName(header)

                    if (rawName.isBlank()) {
                        continue
                    }

                    if (header.isDirectory) {
                        writer.writeDirectory(rawName)
                    } else {
                        writer.writeFile(rawName) { output ->
                            rar.extractFile(header, output)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            throw IOException(
                "RAR extraction failed for ${archive.name}. This archive may be RAR5, encrypted, corrupt, or unsupported by the current extractor: ${t.message}",
                t
            )
        }
    }

    private fun getRarEntryName(header: FileHeader): String {
        val unicodeName = header.fileNameW
        if (!unicodeName.isNullOrBlank()) {
            return unicodeName
        }

        return header.fileNameString ?: ""
    }
}