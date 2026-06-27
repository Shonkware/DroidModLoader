package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.io.IOException

internal object InstallFileCopier {
    private const val BUFFER_SIZE = 64 * 1024

    fun copyRecursively(
        source: File,
        target: File,
        cancellationSignal:
        InstallCancellationSignal =
            InstallCancellationSignal.NONE
    ) {
        cancellationSignal
            .throwIfCancellationRequested()

        when {
            source.isDirectory -> {
                copyDirectory(
                    source = source,
                    target = target,
                    cancellationSignal =
                        cancellationSignal
                )
            }

            source.isFile -> {
                copyFile(
                    source = source,
                    target = target,
                    cancellationSignal =
                        cancellationSignal
                )
            }

            else -> {
                throw IOException(
                    "Installer source does not exist or " +
                            "has an unsupported type: " +
                            source.absolutePath
                )
            }
        }
    }

    private fun copyDirectory(
        source: File,
        target: File,
        cancellationSignal:
        InstallCancellationSignal
    ) {
        cancellationSignal
            .throwIfCancellationRequested()

        if (target.exists() && !target.isDirectory) {
            throw IOException(
                "Installer destination is not a directory: " +
                        target.absolutePath
            )
        }

        if (!target.exists() && !target.mkdirs()) {
            throw IOException(
                "Could not create installer destination: " +
                        target.absolutePath
            )
        }

        val children = source.listFiles()
            ?: throw IOException(
                "Could not read installer source directory: " +
                        source.absolutePath
            )

        children.forEach { child ->
            cancellationSignal
                .throwIfCancellationRequested()

            copyRecursively(
                source = child,
                target = File(target, child.name),
                cancellationSignal =
                    cancellationSignal
            )
        }
    }

    private fun copyFile(
        source: File,
        target: File,
        cancellationSignal:
        InstallCancellationSignal
    ) {
        cancellationSignal
            .throwIfCancellationRequested()

        if (target.exists() && target.isDirectory) {
            throw IOException(
                "Installer file destination is a directory: " +
                        target.absolutePath
            )
        }

        val parent = target.parentFile
            ?: throw IOException(
                "Installer destination has no parent: " +
                        target.absolutePath
            )

        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException(
                "Could not create installer destination parent: " +
                        parent.absolutePath
            )
        }

        try {
            source.inputStream().buffered().use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)

                    while (true) {
                        cancellationSignal
                            .throwIfCancellationRequested()

                        val read = input.read(buffer)

                        if (read < 0) {
                            break
                        }

                        if (read == 0) {
                            continue
                        }

                        cancellationSignal
                            .throwIfCancellationRequested()

                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (exception: Exception) {
            val cleanupFailure =
                removePartialFile(target)

            cleanupFailure?.let(
                exception::addSuppressed
            )

            throw exception
        }
    }

    private fun removePartialFile(
        target: File
    ): IOException? {
        if (!target.exists()) {
            return null
        }

        return try {
            if (target.delete()) {
                null
            } else {
                IOException(
                    "Could not remove partial installer file: " +
                            target.absolutePath
                )
            }
        } catch (exception: Exception) {
            IOException(
                "Could not remove partial installer file: " +
                        target.absolutePath,
                exception
            )
        }
    }
}