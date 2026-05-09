package com.shonkware.droidmodloader.engine.install

import java.io.File

class ModInstaller(
    private val tempDir: File,
    private val modsDir: File,
    private val registry: ArchiveExtractorRegistry = ArchiveExtractorRegistry()
) {

    fun installArchive(archive: File): File {
        val extractor = registry.findExtractor(archive)
        return extractor.extract(archive, tempDir, modsDir)
    }
}