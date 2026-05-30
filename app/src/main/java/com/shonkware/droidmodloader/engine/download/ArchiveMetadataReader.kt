package com.shonkware.droidmodloader.engine.download

import java.io.File
import java.security.MessageDigest

object ArchiveMetadataReader {
    fun detectArchiveFormat(fileName: String): String {
        val lower = fileName.lowercase()

        return when {
            lower.endsWith(".zip") -> "zip"
            lower.endsWith(".7z") -> "7z"
            lower.endsWith(".rar") -> "rar"
            lower.endsWith(".tar") -> "tar"
            lower.endsWith(".tar.gz") -> "tar.gz"
            lower.endsWith(".tgz") -> "tgz"
            lower.endsWith(".tar.bz2") -> "tar.bz2"
            lower.endsWith(".tbz2") -> "tbz2"
            else -> "unknown"
        }
    }

    fun buildFingerprint(file: File): String {
        val raw = "${file.name}|${file.length()}|${file.lastModified()}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    fun cleanDisplayName(fileName: String): String {
        return fileName
            .removeSuffix(".zip")
            .removeSuffix(".7z")
            .removeSuffix(".rar")
            .removeSuffix(".tar")
            .removeSuffix(".tgz")
            .removeSuffix(".tar.gz")
            .removeSuffix(".tar.bz2")
            .removeSuffix(".tbz2")
            .replace('_', ' ')
            .trim()
            .ifBlank { fileName }
    }
}