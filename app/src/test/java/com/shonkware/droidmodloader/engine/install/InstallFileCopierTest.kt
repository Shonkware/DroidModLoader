package com.shonkware.droidmodloader.engine.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class InstallFileCopierTest {
    @Test
    fun `copies directory contents recursively`() {
        withFixture("ordinary") { root ->
            val source = File(root, "source").apply {
                check(mkdirs())
            }
            val target = File(root, "target")

            File(source, "Data/Textures").apply {
                check(mkdirs())
            }
            File(
                source,
                "Data/Textures/example.txt"
            ).writeText("example")

            InstallFileCopier.copyRecursively(
                source = source,
                target = target
            )

            assertEquals(
                "example",
                File(
                    target,
                    "Data/Textures/example.txt"
                ).readText()
            )
        }
    }

    @Test
    fun `cancellation removes partial target file`() {
        withFixture("cancelled") { root ->
            val source = File(
                root,
                "source.bin"
            ).apply {
                writeBytes(
                    ByteArray(256 * 1024) { 1 }
                )
            }
            val target = File(
                root,
                "target.bin"
            )

            var checks = 0
            val signal =
                InstallCancellationSignal {
                    checks++

                    if (checks >= 4) {
                        throw InstallCancelledException()
                    }
                }

            expectCancellation {
                InstallFileCopier.copyRecursively(
                    source = source,
                    target = target,
                    cancellationSignal = signal
                )
            }

            assertFalse(target.exists())
        }
    }

    @Test
    fun `pre-cancelled copy creates no destination`() {
        withFixture("pre-cancelled") { root ->
            val source = File(
                root,
                "source.txt"
            ).apply {
                writeText("example")
            }
            val target = File(
                root,
                "target.txt"
            )
            val controller =
                InstallCancellationController()

            controller.cancel()

            expectCancellation {
                InstallFileCopier.copyRecursively(
                    source = source,
                    target = target,
                    cancellationSignal =
                        controller.signal
                )
            }

            assertFalse(target.exists())
            assertTrue(source.isFile)
        }
    }

    private fun expectCancellation(
        action: () -> Unit
    ): InstallCancelledException {
        try {
            action()
            throw AssertionError(
                "Expected InstallCancelledException"
            )
        } catch (
            exception: InstallCancelledException
        ) {
            return exception
        }
    }

    private fun withFixture(
        name: String,
        action: (File) -> Unit
    ) {
        val root = Files.createTempDirectory(
            "dml-install-copy-$name"
        ).toFile()

        try {
            action(root)
        } finally {
            root.deleteRecursively()
        }
    }
}