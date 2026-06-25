package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.util.zip.ZipInputStream

class ZipArchiveReader : ArchiveReader {

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }

    override fun read(
        archive: File,
        writer: ArchiveEntryWriter
    ) {
        ZipInputStream(archive.inputStream().buffered()).use { zis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var entry = zis.nextEntry

            while (entry != null) {
                if (entry.isDirectory) {
                    writer.writeDirectory(entry.name)
                } else {
                    writer.writeFile(entry.name) { output ->
                        while (true) {
                            val read = zis.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                        }
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}