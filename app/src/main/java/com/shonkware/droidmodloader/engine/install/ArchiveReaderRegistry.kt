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
        val probeResult = formatProbe.probe(archive)

        return when (probeResult.format) {
            ArchiveFormat.ZIP -> zipReader
            ArchiveFormat.SEVEN_Z -> sevenZipReader
            ArchiveFormat.RAR4 -> rarReader

            ArchiveFormat.RAR5 -> {
                throw ArchiveReadException(
                    code = ArchiveReadFailureCode.UNSUPPORTED_VARIANT,
                    message =
                        "RAR5 archives are not supported by this DML release: " +
                                archive.name
                )
            }
        }
    }
}