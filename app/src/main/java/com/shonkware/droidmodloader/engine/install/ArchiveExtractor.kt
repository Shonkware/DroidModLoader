package com.shonkware.droidmodloader.engine.install

import java.io.File

interface ArchiveExtractor {
    fun supports(archive: File): Boolean
    fun extract(archive: File, tempDir: File, modsDir: File): File
}