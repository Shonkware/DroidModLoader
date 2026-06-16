package com.shonkware.droidmodloader.engine.io

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ProfileStoragePathsTest {

    @Test
    fun `active profile id is sanitized for storage`() {
        val paths = ProfileStoragePaths(
            filesDir = File("/internal"),
            activeProfileIdProvider = { "My Profile: TTW" },
            selectedGameIdProvider = { "fallout_nv" }
        )

        assertEquals("My_Profile_TTW", paths.getActiveProfileStorageKey())
    }

    @Test
    fun `missing active profile falls back to selected game`() {
        val paths = ProfileStoragePaths(
            filesDir = File("/internal"),
            activeProfileIdProvider = { null },
            selectedGameIdProvider = { "skyrim_le" }
        )

        assertEquals("unassigned_skyrim_le", paths.getActiveProfileStorageKey())
    }

    @Test
    fun `profile directories use the active storage key`() {
        val paths = ProfileStoragePaths(
            filesDir = File("/internal"),
            activeProfileIdProvider = { "Default Profile" },
            selectedGameIdProvider = { "skyrim_le" }
        )

        assertEquals(
            File("/internal/profiles/Default_Profile"),
            paths.getProfileInternalDir()
        )
        assertEquals(
            File("/external/state/profiles/Default_Profile"),
            paths.getProfileStateDir(File("/external"))
        )
    }
}
