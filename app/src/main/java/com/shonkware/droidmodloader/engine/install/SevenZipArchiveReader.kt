package com.shonkware.droidmodloader.engine.install

import org.apache.commons.compress.MemoryLimitException
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.IOException
import java.util.Locale

class SevenZipArchiveReader internal constructor(
    memoryLimitMiB: Int
) : ArchiveReader {

    constructor() : this(SAFE_MEMORY_LIMIT_MIB)

    companion object {
        private const val BUFFER_SIZE = 256 * 1024
        private const val KIB_PER_MIB = 1024

        const val SAFE_MEMORY_LIMIT_MIB = 256
        const val MAX_MEMORY_LIMIT_MIB = 1024
    }

    private val memoryLimitKiB: Int

    init {
        require(memoryLimitMiB in 1..MAX_MEMORY_LIMIT_MIB) {
            "7z decoder memory limit must be between 1 MiB and " +
                    "$MAX_MEMORY_LIMIT_MIB MiB."
        }

        memoryLimitKiB = memoryLimitMiB * KIB_PER_MIB
    }

    override fun read(
        archive: File,
        writer: ArchiveEntryWriter
    ) {
        try {
            SevenZFile.builder()
                .setFile(archive)
                .setUseDefaultNameForUnnamedEntries(true)
                .setMaxMemoryLimitKiB(memoryLimitKiB)
                .get()
                .use { sevenZ ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var entry = sevenZ.nextEntry

                    while (entry != null) {
                        val currentEntry = entry
                        val entryName = currentEntry.name
                            ?: throw corruptOrUnsupported(
                                archive = archive,
                                cause = IOException(
                                    "7z entry name was null"
                                )
                            )

                        if (currentEntry.isDirectory) {
                            writer.writeDirectory(entryName)
                        } else {
                            writer.writeFile(entryName) { output ->
                                var remaining = currentEntry.size

                                if (remaining >= 0) {
                                    while (remaining > 0) {
                                        val read = readEntryData(
                                            archive = archive,
                                            sevenZ = sevenZ,
                                            buffer = buffer,
                                            length = minOf(
                                                buffer.size.toLong(),
                                                remaining
                                            ).toInt()
                                        )

                                        if (read < 0) {
                                            throw corruptOrUnsupported(
                                                archive = archive,
                                                cause = IOException(
                                                    "Unexpected end of " +
                                                            "7z stream for " +
                                                            "entry: " +
                                                            entryName
                                                )
                                            )
                                        }

                                        if (read == 0) {
                                            continue
                                        }

                                        output.write(
                                            buffer,
                                            0,
                                            read
                                        )
                                        remaining -= read
                                    }
                                } else {
                                    while (true) {
                                        val read = readEntryData(
                                            archive = archive,
                                            sevenZ = sevenZ,
                                            buffer = buffer,
                                            length = buffer.size
                                        )
                                        if (read < 0) break
                                        if (read == 0) continue
                                        output.write(
                                            buffer,
                                            0,
                                            read
                                        )
                                    }
                                }
                            }
                        }

                        entry = sevenZ.nextEntry
                    }
                }
        } catch (exception: ArchiveReadException) {
            throw exception
        } catch (exception: InstallCancelledException) {
            throw exception
        } catch (exception: MemoryLimitException) {
            throw decoderMemoryFailure(
                archive = archive,
                exception = exception
            )
        } catch (exception: PasswordRequiredException) {
            throw encryptedFailure(
                archive = archive,
                exception = exception
            )
        } catch (exception: IOException) {
            throw corruptOrUnsupported(
                archive = archive,
                cause = exception
            )
        }
    }

    private fun readEntryData(
        archive: File,
        sevenZ: SevenZFile,
        buffer: ByteArray,
        length: Int
    ): Int {
        return try {
            sevenZ.read(buffer, 0, length)
        } catch (exception: MemoryLimitException) {
            throw decoderMemoryFailure(
                archive = archive,
                exception = exception
            )
        } catch (exception: PasswordRequiredException) {
            throw encryptedFailure(
                archive = archive,
                exception = exception
            )
        } catch (exception: IOException) {
            throw corruptOrUnsupported(
                archive = archive,
                cause = exception
            )
        }
    }

    private fun decoderMemoryFailure(
        archive: File,
        exception: MemoryLimitException
    ): ArchiveReadException {
        return ArchiveReadException(
            code =
                ArchiveReadFailureCode
                    .DECODER_MEMORY_LIMIT_EXCEEDED,
            message =
                "The 7z archive requires approximately " +
                        "${formatMiB(exception.memoryNeededInKb)} MiB " +
                        "of decoder memory, above DML's safe " +
                        "${formatMiB(exception.memoryLimitInKb.toLong())} " +
                        "MiB limit: ${archive.name}",
            cause = exception
        )
    }

    private fun encryptedFailure(
        archive: File,
        exception: PasswordRequiredException
    ): ArchiveReadException {
        return ArchiveReadException(
            code =
                ArchiveReadFailureCode
                    .PASSWORD_PROTECTED_OR_ENCRYPTED,
            message =
                "Password-protected or encrypted 7z archives are " +
                        "not supported: ${archive.name}",
            cause = exception
        )
    }

    private fun corruptOrUnsupported(
        archive: File,
        cause: Throwable
    ): ArchiveReadException {
        return ArchiveReadException(
            code =
                ArchiveReadFailureCode
                    .CORRUPT_OR_UNSUPPORTED,
            message =
                "The 7z archive is corrupt, truncated, or uses an " +
                        "unsupported compression feature: " +
                        archive.name,
            cause = cause
        )
    }

    private fun formatMiB(
        memoryKiB: Long
    ): String {
        return String.format(
            Locale.ROOT,
            "%.1f",
            memoryKiB.toDouble() / KIB_PER_MIB
        )
    }
}
