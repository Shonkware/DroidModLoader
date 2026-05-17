package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.io.IOException

sealed class ArchiveEntryPathResult {
    data class Valid(val relativePath: String) : ArchiveEntryPathResult()
    data object Ignore : ArchiveEntryPathResult()
}

object ArchiveEntryPath {

    fun normalize(rawPath: String?): ArchiveEntryPathResult {
        if (rawPath.isNullOrBlank()) {
            return ArchiveEntryPathResult.Ignore
        }

        val rawNormalized = rawPath
            .replace("\\", "/")
            .trim()

        if (rawNormalized.startsWith("/")) {
            throw IOException("Blocked absolute archive path: $rawPath")
        }

        if (rawNormalized.matches(Regex("""^[A-Za-z]:/.*"""))) {
            throw IOException("Blocked absolute Windows archive path: $rawPath")
        }

        var path = rawNormalized.replace(Regex("/+"), "/")

        while (path.startsWith("./")) {
            path = path.removePrefix("./")
        }

        path = path.trim('/')

        if (path.isBlank()) {
            return ArchiveEntryPathResult.Ignore
        }

        if (path == ".") {
            return ArchiveEntryPathResult.Ignore
        }

        if (path == ".." ||
            path.startsWith("../") ||
            path.endsWith("/..") ||
            path.contains("/../")
        ) {
            throw IOException("Blocked suspicious archive path: $rawPath")
        }

        if (path.contains("/./") || path.endsWith("/.")) {
            throw IOException("Blocked suspicious archive path with embedded current-directory segment: $rawPath")
        }

        val parts = path.split("/").filter { it.isNotBlank() }

        if (parts.isEmpty()) {
            return ArchiveEntryPathResult.Ignore
        }

        for (part in parts) {
            when (part) {
                "." -> return ArchiveEntryPathResult.Ignore
                ".." -> throw IOException("Blocked suspicious archive path segment: $rawPath")
            }
        }

        return ArchiveEntryPathResult.Valid(parts.joinToString("/"))
    }

    fun safeResolve(root: File, relativePath: String): File {
        val rootCanonical = root.canonicalFile
        val outCanonical = File(rootCanonical, relativePath).canonicalFile

        if (outCanonical != rootCanonical &&
            !outCanonical.path.startsWith(rootCanonical.path + File.separator)
        ) {
            throw IOException("Blocked suspicious archive path: $relativePath")
        }

        return outCanonical
    }
}