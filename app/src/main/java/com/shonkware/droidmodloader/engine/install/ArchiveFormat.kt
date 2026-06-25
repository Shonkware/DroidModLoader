package com.shonkware.droidmodloader.engine.install

import java.io.IOException

enum class ArchiveFormat(
    val expectedExtensions: Set<String>
) {
    ZIP(setOf("zip")),
    SEVEN_Z(setOf("7z")),
    RAR4(setOf("rar")),
    RAR5(setOf("rar"))
}

enum class ArchiveProbeFailureCode {
    FILE_NOT_FOUND,
    FILE_NOT_READABLE,
    UNSUPPORTED_FORMAT
}

data class ArchiveFormatProbeResult(
    val format: ArchiveFormat,
    val fileExtension: String,
    val extensionMismatch: Boolean
)

class ArchiveFormatProbeException(
    val code: ArchiveProbeFailureCode,
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)