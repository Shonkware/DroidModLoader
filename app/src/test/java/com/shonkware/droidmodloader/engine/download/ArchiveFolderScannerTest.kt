package com.shonkware.droidmodloader.engine.download

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArchiveFolderScannerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun scanUsesCanonicalDirectPathsAndFiltersUnsupportedFiles() {
        val directory =
            temporaryFolder.newFolder("archives")

        val zip =
            directory.resolve("Example.zip").apply {
                writeSignature(
                    0x50, 0x4B, 0x03, 0x04
                )
            }

        directory.resolve("readme.txt")
            .writeText("text")

        val result =
            ArchiveFolderScanner().scan(directory.path)

        assertEquals(directory.name, result.folderName)
        assertEquals(1, result.entries.size)
        assertEquals(
            zip.canonicalPath,
            result.entries.single().sourcePath
        )
        assertEquals(
            zip.canonicalPath,
            result.entries.single().stableId
        )
        assertEquals(
            "zip",
            result.entries.single().archiveFormat
        )
    }

    @Test
    fun scanUsesDetectedFormatInsteadOfExtension() {
        val directory =
            temporaryFolder.newFolder(
                "mismatched-extension"
            )

        val renamedZip =
            directory.resolve("Renamed.7z").apply {
                writeSignature(
                    0x50, 0x4B, 0x03, 0x04
                )
            }

        val result =
            ArchiveFolderScanner().scan(directory.path)

        assertEquals(1, result.entries.size)
        assertEquals(
            renamedZip.canonicalPath,
            result.entries.single().sourcePath
        )
        assertEquals(
            "zip",
            result.entries.single().archiveFormat
        )
    }

    @Test
    fun scanIncludesRecognizedArchiveWithoutExtension() {
        val directory =
            temporaryFolder.newFolder(
                "missing-extension"
            )

        directory.resolve("Archive").apply {
            writeSignature(
                0x37, 0x7A, 0xBC, 0xAF,
                0x27, 0x1C, 0x00, 0x04
            )
        }

        val result =
            ArchiveFolderScanner().scan(directory.path)

        assertEquals(1, result.entries.size)
        assertEquals(
            "7z",
            result.entries.single().archiveFormat
        )
    }

    private fun File.writeSignature(
        vararg values: Int
    ) {
        writeBytes(
            values.map {
                it.toByte()
            }.toByteArray()
        )
    }
}
