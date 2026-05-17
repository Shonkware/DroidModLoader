package com.shonkware.droidmodloader.engine.install

import java.io.File

interface ArchiveReader {
    fun supports(archive: File): Boolean

    fun read(
        archive: File,
        writer: ArchiveEntryWriter
    )
}