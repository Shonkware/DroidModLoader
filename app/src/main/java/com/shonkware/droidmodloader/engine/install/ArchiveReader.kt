package com.shonkware.droidmodloader.engine.install

import java.io.File

interface ArchiveReader {
    fun read(
        archive: File,
        writer: ArchiveEntryWriter
    )
}