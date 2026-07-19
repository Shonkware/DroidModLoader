package com.shonkware.droidmodloader.engine.install

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

class ZipArchiveReader : ArchiveReader {

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }

    override fun read(
        archive: File,
        writer: ArchiveEntryWriter
    ) {
        try {
            ZipInputStream(
                archive.inputStream().buffered()
            ).use { zip ->
                val buffer = ByteArray(BUFFER_SIZE)
                var entry = zip.nextEntry

                while (entry != null) {
                    if (entry.isDirectory) {
                        writer.writeDirectory(entry.name)
                    } else {
                        writer.writeFile(entry.name) { output ->
                            while (true) {
                                val read = readEntryData(
                                    archive = archive,
                                    zip = zip,
                                    buffer = buffer
                                )
                                if (read < 0) break
                                if (read == 0) continue
                                output.write(buffer, 0, read)
                            }
                        }
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (exception: ArchiveReadException) {
            throw exception
        } catch (exception: InstallCancelledException) {
            throw exception
        } catch (exception: ZipException) {
            throw corruptOrUnsupported(
                archive = archive,
                cause = exception
            )
        } catch (exception: EOFException) {
            throw truncated(
                archive = archive,
                cause = exception
            )
        } catch (exception: IOException) {
            throw ArchiveReadException(
                code = ArchiveReadFailureCode.IO_FAILURE,
                message =
                    "DML could not read ZIP archive data: " +
                            archive.name,
                cause = exception
            )
        }
    }

    private fun readEntryData(
        archive: File,
        zip: ZipInputStream,
        buffer: ByteArray
    ): Int {
        return try {
            zip.read(buffer)
        } catch (exception: ZipException) {
            throw corruptOrUnsupported(
                archive = archive,
                cause = exception
            )
        } catch (exception: EOFException) {
            throw truncated(
                archive = archive,
                cause = exception
            )
        }
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
                "The ZIP archive is corrupt, truncated, encrypted, " +
                        "or uses an unsupported compression method: " +
                        archive.name,
            cause = cause
        )
    }

    private fun truncated(
        archive: File,
        cause: Throwable
    ): ArchiveReadException {
        return ArchiveReadException(
            code =
                ArchiveReadFailureCode
                    .CORRUPT_OR_UNSUPPORTED,
            message =
                "The ZIP archive ended unexpectedly and may be " +
                        "truncated: ${archive.name}",
            cause = cause
        )
    }
}
