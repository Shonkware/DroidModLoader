package com.shonkware.droidmodloader.engine.install

import android.util.Log
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.Normalizer
import java.util.Locale

class ArchiveEntryWriter internal constructor(
    private val outputDir: File,
    private val limits: ExtractionLimits,
    private val debugLog: (String) -> Unit
) {
    constructor(
        outputDir: File,
        limits: ExtractionLimits = ExtractionLimits()
    ) : this(
        outputDir = outputDir,
        limits = limits,
        debugLog = { message ->
            Log.d(TAG, message)
        }
    )

    companion object {
        private const val TAG = "DroidModLoader"
    }

    private val pathsByComparisonKey =
        mutableMapOf<String, String>()

    private var acceptedEntryCount = 0
    private var totalBytesWritten = 0L

    fun writeDirectory(rawEntryName: String) {
        val entry = resolveEntryOrNull(rawEntryName)
            ?: return

        if (entry.outputFile.exists()) {
            if (!entry.outputFile.isDirectory) {
                throw ioFailure(
                    "Archive directory conflicts with an existing file: " +
                            entry.relativePath
                )
            }

            return
        }

        if (!entry.outputFile.mkdirs()) {
            throw ioFailure(
                "Could not create extracted directory: " +
                        entry.relativePath
            )
        }
    }

    fun writeFile(
        rawEntryName: String,
        writeContent: (OutputStream) -> Unit
    ) {
        val entry = resolveEntryOrNull(rawEntryName)
            ?: return

        val parent = entry.outputFile.parentFile
            ?: throw ioFailure(
                "Extracted file has no parent directory: " +
                        entry.relativePath
            )

        ensureDirectoryExists(
            directory = parent,
            relativePath = entry.relativePath
        )

        if (entry.outputFile.exists()) {
            throw ArchiveReadException(
                code =
                    ArchiveReadFailureCode.DUPLICATE_ENTRY_PATH,
                message =
                    "Archive output already exists for entry: " +
                            entry.relativePath
            )
        }

        try {
            entry.outputFile.outputStream().use { output ->
                writeContent(
                    LimitEnforcingOutputStream(
                        delegate = output,
                        relativePath = entry.relativePath
                    )
                )
            }
        } catch (exception: Exception) {
            val cleanupFailure =
                removePartialFile(entry.outputFile)

            when (exception) {
                is ArchiveReadException -> {
                    cleanupFailure?.let(
                        exception::addSuppressed
                    )
                    throw exception
                }

                is IOException -> {
                    val wrapped = ioFailure(
                        message =
                            "Could not write extracted archive entry: " +
                                    entry.relativePath,
                        cause = exception
                    )
                    cleanupFailure?.let(
                        wrapped::addSuppressed
                    )
                    throw wrapped
                }

                else -> {
                    cleanupFailure?.let(
                        exception::addSuppressed
                    )
                    throw exception
                }
            }
        }
    }

    private fun resolveEntryOrNull(
        rawEntryName: String
    ): ResolvedArchiveEntry? {
        val normalized = try {
            ArchiveEntryPath.normalize(rawEntryName)
        } catch (exception: IOException) {
            throw ArchiveReadException(
                code =
                    ArchiveReadFailureCode.UNSAFE_ENTRY_PATH,
                message =
                    "Blocked unsafe archive entry path: " +
                            rawEntryName,
                cause = exception
            )
        }

        if (normalized is ArchiveEntryPathResult.Ignore) {
            debugLog(
                "Ignored archive metadata/root entry: " +
                        rawEntryName
            )
            return null
        }

        val relativePath =
            (normalized as ArchiveEntryPathResult.Valid)
                .relativePath

        if (
            relativePath.length >
            limits.maxRelativePathCharacters
        ) {
            throw ArchiveReadException(
                code =
                    ArchiveReadFailureCode
                        .PATH_LENGTH_LIMIT_EXCEEDED,
                message =
                    "Archive entry path exceeds DML's limit: " +
                            relativePath
            )
        }

        val outputFile = try {
            ArchiveEntryPath.safeResolve(
                root = outputDir,
                relativePath = relativePath
            )
        } catch (exception: IOException) {
            throw ArchiveReadException(
                code =
                    ArchiveReadFailureCode.UNSAFE_ENTRY_PATH,
                message =
                    "Blocked unsafe archive entry path: " +
                            relativePath,
                cause = exception
            )
        }

        registerEntryPath(relativePath)

        return ResolvedArchiveEntry(
            relativePath = relativePath,
            outputFile = outputFile
        )
    }

    private fun registerEntryPath(
        relativePath: String
    ) {
        val comparisonKey = Normalizer
            .normalize(
                relativePath,
                Normalizer.Form.NFC
            )
            .lowercase(Locale.ROOT)

        val existingPath =
            pathsByComparisonKey[comparisonKey]

        if (existingPath != null) {
            val failureCode =
                if (existingPath == relativePath) {
                    ArchiveReadFailureCode
                        .DUPLICATE_ENTRY_PATH
                } else {
                    ArchiveReadFailureCode.CASE_COLLISION
                }

            throw ArchiveReadException(
                code = failureCode,
                message =
                    "Archive entries resolve to the same output path: " +
                            "$existingPath and $relativePath"
            )
        }

        if (
            acceptedEntryCount >=
            limits.maxEntries
        ) {
            throw ArchiveReadException(
                code =
                    ArchiveReadFailureCode
                        .ENTRY_LIMIT_EXCEEDED,
                message =
                    "Archive contains more than " +
                            "${limits.maxEntries} entries."
            )
        }

        pathsByComparisonKey[comparisonKey] =
            relativePath
        acceptedEntryCount += 1
    }

    private fun ensureDirectoryExists(
        directory: File,
        relativePath: String
    ) {
        if (directory.exists()) {
            if (!directory.isDirectory) {
                throw ioFailure(
                    "Archive entry parent is not a directory: " +
                            relativePath
                )
            }

            return
        }

        if (!directory.mkdirs()) {
            throw ioFailure(
                "Could not create extracted-entry parent: " +
                        relativePath
            )
        }
    }

    private fun removePartialFile(
        outputFile: File
    ): IOException? {
        if (!outputFile.exists()) {
            return null
        }

        return try {
            if (outputFile.delete()) {
                null
            } else {
                IOException(
                    "Could not remove partial extracted file: " +
                            outputFile.absolutePath
                )
            }
        } catch (exception: Exception) {
            IOException(
                "Could not remove partial extracted file: " +
                        outputFile.absolutePath,
                exception
            )
        }
    }

    private fun ioFailure(
        message: String,
        cause: Throwable? = null
    ): ArchiveReadException {
        return ArchiveReadException(
            code = ArchiveReadFailureCode.IO_FAILURE,
            message = message,
            cause = cause
        )
    }

    private data class ResolvedArchiveEntry(
        val relativePath: String,
        val outputFile: File
    )

    private inner class LimitEnforcingOutputStream(
        private val delegate: OutputStream,
        private val relativePath: String
    ) : OutputStream() {
        private var fileBytesWritten = 0L

        override fun write(value: Int) {
            ensureWriteAllowed(1L)
            delegate.write(value)
            recordWrittenBytes(1L)
        }

        override fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int
        ) {
            if (
                offset < 0 ||
                length < 0 ||
                offset > buffer.size - length
            ) {
                throw IndexOutOfBoundsException()
            }

            if (length == 0) {
                return
            }

            val byteCount = length.toLong()

            ensureWriteAllowed(byteCount)
            delegate.write(buffer, offset, length)
            recordWrittenBytes(byteCount)
        }

        override fun flush() {
            delegate.flush()
        }

        override fun close() {
            delegate.close()
        }

        private fun ensureWriteAllowed(
            additionalBytes: Long
        ) {
            if (
                additionalBytes >
                limits.maxFileBytes -
                fileBytesWritten
            ) {
                throw ArchiveReadException(
                    code =
                        ArchiveReadFailureCode
                            .FILE_SIZE_LIMIT_EXCEEDED,
                    message =
                        "Archive entry exceeds DML's per-file " +
                                "extraction limit: $relativePath"
                )
            }

            if (
                additionalBytes >
                limits.maxTotalBytes -
                totalBytesWritten
            ) {
                throw ArchiveReadException(
                    code =
                        ArchiveReadFailureCode
                            .TOTAL_SIZE_LIMIT_EXCEEDED,
                    message =
                        "Archive exceeds DML's total " +
                                "extraction limit."
                )
            }
        }

        private fun recordWrittenBytes(
            byteCount: Long
        ) {
            fileBytesWritten += byteCount
            totalBytesWritten += byteCount
        }
    }
}