package com.shonkware.droidmodloader.engine.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class RarArchiveReaderTest {
    @Test
    fun `ordinary rar entries are written through the archive writer`() {
        withFixture("ordinary") { fixture ->
            val session = FakeRarArchiveSession(
                entries = mutableListOf(
                    directoryEntry("Data"),
                    fileEntry(
                        name = "Data/example.txt",
                        content = "example"
                    )
                )
            )
            val reader = reader(session)

            reader.read(
                archive = fixture.archive,
                writer = ArchiveEntryWriter(fixture.outputDir)
            )

            assertTrue(
                File(fixture.outputDir, "Data").isDirectory
            )
            assertEquals(
                "example",
                File(
                    fixture.outputDir,
                    "Data/example.txt"
                ).readText()
            )
            assertTrue(session.closed)
        }
    }

    @Test
    fun `encrypted archive fails with a precise code`() {
        withFixture("encrypted-archive") { fixture ->
            val session = FakeRarArchiveSession(
                encrypted = true
            )
            val reader = reader(session)

            val failure = expectReadFailure {
                reader.read(
                    archive = fixture.archive,
                    writer = ArchiveEntryWriter(
                        fixture.outputDir
                    )
                )
            }

            assertEquals(
                ArchiveReadFailureCode
                    .PASSWORD_PROTECTED_OR_ENCRYPTED,
                failure.code
            )
            assertTrue(session.closed)
            assertTrue(
                fixture.outputDir.listFiles()
                    .orEmpty()
                    .isEmpty()
            )
        }
    }

    @Test
    fun `encrypted entry fails before it is written`() {
        withFixture("encrypted-entry") { fixture ->
            val session = FakeRarArchiveSession(
                entries = mutableListOf(
                    fileEntry(
                        name = "secret.txt",
                        content = "secret",
                        encrypted = true
                    )
                )
            )
            val reader = reader(session)

            val failure = expectReadFailure {
                reader.read(
                    archive = fixture.archive,
                    writer = ArchiveEntryWriter(
                        fixture.outputDir
                    )
                )
            }

            assertEquals(
                ArchiveReadFailureCode
                    .PASSWORD_PROTECTED_OR_ENCRYPTED,
                failure.code
            )
            assertFalse(
                File(
                    fixture.outputDir,
                    "secret.txt"
                ).exists()
            )
            assertTrue(session.closed)
        }
    }

    @Test
    fun `split entry fails as multipart`() {
        withFixture("multipart") { fixture ->
            val session = FakeRarArchiveSession(
                entries = mutableListOf(
                    fileEntry(
                        name = "continued.bin",
                        content = "partial",
                        splitAfter = true
                    )
                )
            )
            val reader = reader(session)

            val failure = expectReadFailure {
                reader.read(
                    archive = fixture.archive,
                    writer = ArchiveEntryWriter(
                        fixture.outputDir
                    )
                )
            }

            assertEquals(
                ArchiveReadFailureCode.MULTIPART_ARCHIVE,
                failure.code
            )
            assertFalse(
                File(
                    fixture.outputDir,
                    "continued.bin"
                ).exists()
            )
            assertTrue(session.closed)
        }
    }

    @Test
    fun `session io failure receives the io failure code`() {
        withFixture("io-failure") { fixture ->
            val reader = RarArchiveReader {
                throw IOException(
                    "Forced archive I/O failure"
                )
            }

            val failure = expectReadFailure {
                reader.read(
                    archive = fixture.archive,
                    writer = ArchiveEntryWriter(
                        fixture.outputDir
                    )
                )
            }

            assertEquals(
                ArchiveReadFailureCode.IO_FAILURE,
                failure.code
            )
        }
    }

    @Test
    fun `unknown junrar failure is reported without speculation`() {
        withFixture("unknown-failure") { fixture ->
            val session = FakeRarArchiveSession(
                nextEntryFailure =
                    IllegalStateException(
                        "Forced decoder failure"
                    )
            )
            val reader = reader(session)

            val failure = expectReadFailure {
                reader.read(
                    archive = fixture.archive,
                    writer = ArchiveEntryWriter(
                        fixture.outputDir
                    )
                )
            }

            assertEquals(
                ArchiveReadFailureCode
                    .CORRUPT_OR_UNSUPPORTED,
                failure.code
            )
            assertTrue(
                failure.message.orEmpty().contains(
                    "corrupt or uses an unsupported"
                )
            )
            assertTrue(session.closed)
        }
    }

    private fun reader(
        session: FakeRarArchiveSession
    ): RarArchiveReader {
        return RarArchiveReader {
            session
        }
    }

    private fun directoryEntry(
        name: String
    ): RarArchiveEntry {
        return RarArchiveEntry(
            name = name,
            directory = true,
            encrypted = false,
            splitBefore = false,
            splitAfter = false,
            writeContent = {}
        )
    }

    private fun fileEntry(
        name: String,
        content: String,
        encrypted: Boolean = false,
        splitBefore: Boolean = false,
        splitAfter: Boolean = false
    ): RarArchiveEntry {
        return RarArchiveEntry(
            name = name,
            directory = false,
            encrypted = encrypted,
            splitBefore = splitBefore,
            splitAfter = splitAfter,
            writeContent = { output ->
                output.write(
                    content.toByteArray()
                )
            }
        )
    }

    private fun expectReadFailure(
        action: () -> Unit
    ): ArchiveReadException {
        try {
            action()
            throw AssertionError(
                "Expected ArchiveReadException"
            )
        } catch (exception: ArchiveReadException) {
            return exception
        }
    }

    private fun withFixture(
        name: String,
        action: (Fixture) -> Unit
    ) {
        val root = Files.createTempDirectory(
            "dml-rar-reader-$name"
        ).toFile()
        val archive = File(
            root,
            "example.rar"
        ).apply {
            writeText("synthetic")
        }
        val outputDir = File(
            root,
            "output"
        ).apply {
            check(mkdirs())
        }

        try {
            action(
                Fixture(
                    root = root,
                    archive = archive,
                    outputDir = outputDir
                )
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private data class Fixture(
        val root: File,
        val archive: File,
        val outputDir: File
    )

    private class FakeRarArchiveSession(
        override val encrypted: Boolean = false,
        private val entries:
        MutableList<RarArchiveEntry> =
            mutableListOf(),
        private val nextEntryFailure:
        Exception? = null
    ) : RarArchiveSession {
        var closed: Boolean = false
            private set

        override fun nextEntry(): RarArchiveEntry? {
            nextEntryFailure?.let { throw it }

            if (entries.isEmpty()) {
                return null
            }

            return entries.removeAt(0)
        }

        override fun close() {
            closed = true
        }
    }
}