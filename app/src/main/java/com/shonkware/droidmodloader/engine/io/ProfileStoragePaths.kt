package com.shonkware.droidmodloader.engine.io

import java.io.File

internal class ProfileStoragePaths(
    private val filesDir: File,
    private val activeProfileIdProvider: () -> String?,
    private val selectedGameIdProvider: () -> String
) {

    fun getActiveProfileStorageKey(): String {
        val profileId = activeProfileIdProvider()

        return if (!profileId.isNullOrBlank()) {
            sanitizeStorageName(profileId)
        } else {
            sanitizeStorageName("unassigned_${selectedGameIdProvider()}")
        }
    }

    fun getProfileInternalDir(): File {
        return File(filesDir, "profiles/${getActiveProfileStorageKey()}")
    }

    fun getProfileStateDir(externalBaseDir: File): File {
        return File(externalBaseDir, "state/profiles/${getActiveProfileStorageKey()}")
    }

    private fun sanitizeStorageName(value: String): String {
        return value
            .replace(Regex("""[^A-Za-z0-9._-]+"""), "_")
            .trim('_')
            .ifBlank { "default" }
    }
}
