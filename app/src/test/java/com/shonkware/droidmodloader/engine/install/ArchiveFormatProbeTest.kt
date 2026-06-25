package com.shonkware.droidmodloader.engine.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArchiveFormatProbeTest {
    private val probe = ArchiveFormatProbe()

    @Test
    fun `detects ordinary zip signature`() {
        withRoot("zip") { root ->
            val archive = writeArchive(
                root = root,
                name = "example.zip",
                bytes = bytes(
                    0x50, 0x4B, 0x03, 0x04,
                    0x00, 0x00, 0x00, 0x00
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.ZIP, result.format)
            assertEquals("zip", result.fileExtension)
            assertFalse(result.extensionMismatch)
        }
    }

    @Test
    fun `detects empty zip signature`() {
        withRoot("empty-zip") { root ->
            val archive = writeArchive(
                root = root,
                name = "empty.zip",
                bytes = bytes(
                    0x50, 0x4B, 0x05, 0x06
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.ZIP, result.format)
            assertFalse(result.extensionMismatch)
        }
    }

    @Test
    fun `detects seven z signature`() {
        withRoot("seven-z") { root ->
            val archive = writeArchive(
                root = root,
                name = "example.7z",
                bytes = bytes(
                    0x37, 0x7A, 0xBC, 0xAF,
                    0x27, 0x1C, 0x00, 0x04
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.SEVEN_Z, result.format)
            assertEquals("7z", result.fileExtension)
            assertFalse(result.extensionMismatch)
        }
    }

    @Test
    fun `detects rar4 signature`() {
        withRoot("rar4") { root ->
            val archive = writeArchive(
                root = root,
                name = "example.rar",
                bytes = bytes(
                    0x52, 0x61, 0x72, 0x21,
                    0x1A, 0x07, 0x00
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.RAR4, result.format)
            assertFalse(result.extensionMismatch)
        }
    }

    @Test
    fun `detects rar5 signature`() {
        withRoot("rar5") { root ->
            val archive = writeArchive(
                root = root,
                name = "example.rar",
                bytes = bytes(
                    0x52, 0x61, 0x72, 0x21,
                    0x1A, 0x07, 0x01, 0x00
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.RAR5, result.format)
            assertFalse(result.extensionMismatch)
        }
    }

    @Test
    fun `known content with the wrong extension reports mismatch`() {
        withRoot("mismatch") { root ->
            val archive = writeArchive(
                root = root,
                name = "renamed.zip",
                bytes = bytes(
                    0x37, 0x7A, 0xBC, 0xAF,
                    0x27, 0x1C, 0x00, 0x04
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.SEVEN_Z, result.format)
            assertEquals("zip", result.fileExtension)
            assertTrue(result.extensionMismatch)
        }
    }

    @Test
    fun `recognized content without an extension reports mismatch`() {
        withRoot("no-extension") { root ->
            val archive = writeArchive(
                root = root,
                name = "archive",
                bytes = bytes(
                    0x50, 0x4B, 0x03, 0x04
                )
            )

            val result = probe.probe(archive)

            assertEquals(ArchiveFormat.ZIP, result.format)
            assertEquals("", result.fileExtension)
            assertTrue(result.extensionMismatch)
        }
    }

    @Test
    fun `unknown bytes fail with unsupported format`() {
        withRoot("unknown") { root ->
            val archive = writeArchive(
                root = root,
                name = "unknown.zip",
                bytes = bytes(
                    0x00, 0x01, 0x02, 0x03,
                    0x04, 0x05, 0x06, 0x07
                )
            )

            val failure = expectProbeFailure {
                probe.probe(archive)
            }

            assertEquals(
                ArchiveProbeFailureCode.UNSUPPORTED_FORMAT,
                failure.code
            )
        }
    }

    @Test
    fun `short files fail safely without indexing past their data`() {
        withRoot("short") { root ->
            val archive = writeArchive(
                root = root,
                name = "short.rar",
                bytes = bytes(0x52, 0x61, 0x72)
            )

            val failure = expectProbeFailure {
                probe.probe(archive)
            }

            assertEquals(
                ArchiveProbeFailureCode.UNSUPPORTED_FORMAT,
                failure.code
            )
        }
    }

    @Test
    fun `missing files have a distinct failure code`() {
        withRoot("missing") { root ->
            val missing = File(root, "missing.zip")

            val failure = expectProbeFailure {
                probe.probe(missing)
            }

            assertEquals(
                ArchiveProbeFailureCode.FILE_NOT_FOUND,
                failure.code
            )
        }
    }

    @Test
    fun `directories are not accepted as archive files`() {
        withRoot("directory") { root ->
            val directory = File(root, "archive.zip").apply {
                check(mkdirs())
            }

            val failure = expectProbeFailure {
                probe.probe(directory)
            }

            assertEquals(
                ArchiveProbeFailureCode.FILE_NOT_READABLE,
                failure.code
            )
        }
    }

    private fun writeArchive(
        root: File,
        name: String,
        bytes: ByteArray
    ): File {
        return File(root, name).apply {
            writeBytes(bytes)
        }
    }

    private fun bytes(vararg values: Int): ByteArray {
        return values.map(Int::toByte).toByteArray()
    }

    private fun expectProbeFailure(
        action: () -> Unit
    ): ArchiveFormatProbeException {
        try {
            action()
            throw AssertionError(
                "Expected ArchiveFormatProbeException"
            )
        } catch (exception: ArchiveFormatProbeException) {
            return exception
        }
    }

    private fun withRoot(
        name: String,
        action: (File) -> Unit
    ) {
        val root = Files.createTempDirectory(
            "dml-archive-probe-$name"
        ).toFile()

        try {
            action(root)
        } finally {
            root.deleteRecursively()
        }
    }
}