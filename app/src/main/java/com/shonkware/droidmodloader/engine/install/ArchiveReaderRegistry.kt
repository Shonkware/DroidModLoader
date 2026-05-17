package com.shonkware.droidmodloader.engine.install

import java.io.File

class ArchiveReaderRegistry(
    private val readers: List<ArchiveReader> = listOf(
        ZipArchiveReader(),
        SevenZipArchiveReader(),
        RarArchiveReader()
    )
) {
    fun findReader(archive: File): ArchiveReader {
        return readers.firstOrNull { it.supports(archive) }
            ?: throw IllegalArgumentException(
                "Unsupported archive format: ${archive.name}. Supported formats: ZIP, 7Z, RAR"
            )
    }
}