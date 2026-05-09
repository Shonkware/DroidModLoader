package com.shonkware.droidmodloader.engine.install

import java.io.File

class ArchiveExtractorRegistry(
    private val extractors: List<ArchiveExtractor> = listOf(
        ZipArchiveExtractor(),
        SevenZipArchiveExtractor()
    )
) {

    fun findExtractor(archive: File): ArchiveExtractor {
        return extractors.firstOrNull { it.supports(archive) }
            ?: throw IllegalArgumentException("Unsupported archive format: ${archive.name}")
    }
}