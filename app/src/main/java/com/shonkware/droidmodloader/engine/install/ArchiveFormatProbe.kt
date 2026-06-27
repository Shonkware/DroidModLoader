package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.io.IOException
import java.util.Locale

class ArchiveFormatProbe {
    companion object {
        private const val MAX_SIGNATURE_LENGTH = 8

        private val ZIP_LOCAL_FILE_SIGNATURE = byteArrayOf(
            0x50, 0x4B, 0x03, 0x04
        )

        private val ZIP_EMPTY_ARCHIVE_SIGNATURE = byteArrayOf(
            0x50, 0x4B, 0x05, 0x06
        )

        private val ZIP_SPANNED_SIGNATURE = byteArrayOf(
            0x50, 0x4B, 0x07, 0x08
        )

        private val SEVEN_Z_SIGNATURE = byteArrayOf(
            0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(),
            0x27, 0x1C
        )

        private val RAR4_SIGNATURE = byteArrayOf(
            0x52, 0x61, 0x72, 0x21,
            0x1A, 0x07, 0x00
        )

        private val RAR5_SIGNATURE = byteArrayOf(
            0x52, 0x61, 0x72, 0x21,
            0x1A, 0x07, 0x01, 0x00
        )
    }

    fun probe(archive: File): ArchiveFormatProbeResult {
        validateArchiveFile(archive)

        val prefix = try {
            readPrefix(archive)
        } catch (exception: IOException) {
            throw ArchiveFormatProbeException(
                code = ArchiveProbeFailureCode.FILE_NOT_READABLE,
                message = "DML could not read archive data from ${archive.name}.",
                cause = exception
            )
        }

        val format = detectFormat(prefix)
            ?: throw ArchiveFormatProbeException(
                code = ArchiveProbeFailureCode.UNSUPPORTED_FORMAT,
                message = "Unsupported or unrecognized archive format: ${archive.name}."
            )

        val extension = archive.extension.lowercase(Locale.ROOT)

        return ArchiveFormatProbeResult(
            format = format,
            fileExtension = extension,
            extensionMismatch =
                extension !in format.expectedExtensions
        )
    }

    private fun validateArchiveFile(archive: File) {
        if (!archive.exists()) {
            throw ArchiveFormatProbeException(
                code = ArchiveProbeFailureCode.FILE_NOT_FOUND,
                message = "Archive file does not exist: ${archive.name}."
            )
        }

        if (!archive.isFile || !archive.canRead()) {
            throw ArchiveFormatProbeException(
                code = ArchiveProbeFailureCode.FILE_NOT_READABLE,
                message = "Archive file cannot be read: ${archive.name}."
            )
        }
    }

    private fun readPrefix(archive: File): ByteArray {
        val prefix = ByteArray(MAX_SIGNATURE_LENGTH)
        var totalRead = 0

        archive.inputStream().buffered().use { input ->
            while (totalRead < prefix.size) {
                val read = input.read(
                    prefix,
                    totalRead,
                    prefix.size - totalRead
                )

                if (read < 0) {
                    break
                }

                if (read == 0) {
                    continue
                }

                totalRead += read
            }
        }

        return prefix.copyOf(totalRead)
    }

    private fun detectFormat(prefix: ByteArray): ArchiveFormat? {
        return when {
            prefix.startsWith(RAR5_SIGNATURE) -> ArchiveFormat.RAR5
            prefix.startsWith(RAR4_SIGNATURE) -> ArchiveFormat.RAR4
            prefix.startsWith(SEVEN_Z_SIGNATURE) -> ArchiveFormat.SEVEN_Z

            prefix.startsWith(ZIP_LOCAL_FILE_SIGNATURE) ||
                    prefix.startsWith(ZIP_EMPTY_ARCHIVE_SIGNATURE) ||
                    prefix.startsWith(ZIP_SPANNED_SIGNATURE) -> {
                ArchiveFormat.ZIP
            }

            else -> null
        }
    }

    private fun ByteArray.startsWith(
        signature: ByteArray
    ): Boolean {
        if (size < signature.size) {
            return false
        }

        return signature.indices.all { index ->
            this[index] == signature[index]
        }
    }
}