package com.shonkware.droidmodloader.engine.install

import java.io.File

class ArchiveExtractor(
    private val readerRegistry: ArchiveReaderRegistry = ArchiveReaderRegistry()
) {
    fun extractToRawFolder(
        archive: File,
        outputDir: File,
        cancellationSignal:
        InstallCancellationSignal =
            InstallCancellationSignal.NONE
    ) {
        cancellationSignal
            .throwIfCancellationRequested()

        val reader = readerRegistry.findReader(archive)
        val writer = ArchiveEntryWriter(
            outputDir = outputDir,
            cancellationSignal =
                cancellationSignal
        )

        reader.read(
            archive = archive,
            writer = writer
        )

        cancellationSignal
            .throwIfCancellationRequested()
    }
}