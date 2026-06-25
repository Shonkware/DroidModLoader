package com.shonkware.droidmodloader.engine.install

import java.io.File

class ArchiveReaderRegistry internal constructor(
    private val formatProbe: ArchiveFormatProbe,
    private val zipReader: ArchiveReader,
    private val sevenZipReader: ArchiveReader,
    private val rarReader: ArchiveReader
) {
    constructor() : this(
        formatProbe = ArchiveFormatProbe(),
        zipReader = ZipArchiveReader(),
        sevenZipReader = SevenZipArchiveReader(),
        rarReader = RarArchiveReader()
    )

    fun findReader(archive: File): ArchiveReader {
        return when (formatProbe.probe(archive).format) {
            ArchiveFormat.ZIP -> zipReader
            ArchiveFormat.SEVEN_Z -> sevenZipReader
            ArchiveFormat.RAR4,
            ArchiveFormat.RAR5 -> rarReader
        }
    }
}