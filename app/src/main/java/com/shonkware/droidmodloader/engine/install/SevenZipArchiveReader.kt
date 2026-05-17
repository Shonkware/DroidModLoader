package com.shonkware.droidmodloader.engine.install

import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.IOException

class SevenZipArchiveReader : ArchiveReader {

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val SEVEN_Z_MEMORY_LIMIT_KIB = 128 * 1024
    }

    override fun supports(archive: File): Boolean {
        return archive.extension.lowercase() == "7z"
    }

    override fun read(
        archive: File,
        writer: ArchiveEntryWriter
    ) {
        SevenZFile.builder()
            .setFile(archive)
            .setUseDefaultNameForUnnamedEntries(true)
            .setMaxMemoryLimitKiB(SEVEN_Z_MEMORY_LIMIT_KIB)
            .get()
            .use { sevenZ ->
                val buffer = ByteArray(BUFFER_SIZE)
                var entry = sevenZ.nextEntry

                while (entry != null) {
                    val entryName = entry.name
                        ?: throw IOException("7z entry name was null")

                    if (entry.isDirectory) {
                        writer.writeDirectory(entryName)
                    } else {
                        writer.writeFile(entryName) { output ->
                            var remaining = entry.size

                            if (remaining >= 0) {
                                while (remaining > 0) {
                                    val read = sevenZ.read(
                                        buffer,
                                        0,
                                        minOf(buffer.size.toLong(), remaining).toInt()
                                    )

                                    if (read < 0) {
                                        throw IOException("Unexpected end of 7z stream for entry: $entryName")
                                    }

                                    if (read == 0) {
                                        continue
                                    }

                                    output.write(buffer, 0, read)
                                    remaining -= read
                                }
                            } else {
                                while (true) {
                                    val read = sevenZ.read(buffer)
                                    if (read < 0) break
                                    if (read == 0) continue
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    }

                    entry = sevenZ.nextEntry
                }
            }
    }
}