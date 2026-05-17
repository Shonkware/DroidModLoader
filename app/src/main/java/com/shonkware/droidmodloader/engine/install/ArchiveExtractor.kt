package com.shonkware.droidmodloader.engine.install

import java.io.File

class ArchiveExtractor(
    private val readerRegistry: ArchiveReaderRegistry = ArchiveReaderRegistry()
) {
    fun extractToRawFolder(
        archive: File,
        outputDir: File
    ) {
        val reader = readerRegistry.findReader(archive)
        val writer = ArchiveEntryWriter(outputDir)

        reader.read(
            archive = archive,
            writer = writer
        )
    }
}