package com.shonkware.droidmodloader.engine.plugins

import com.shonkware.droidmodloader.engine.model.PluginEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PluginTimestampOrdererTest {
    @Test
    fun applyUsesStrictlyIncreasingTimestampsInPriorityOrder() {
        withDataFolder("timestamp-order") { dataFolder ->
            val first = pluginFile(dataFolder, "First.esm", 10_000L)
            val disabled = pluginFile(dataFolder, "Disabled.esp", 20_000L)
            val last = pluginFile(dataFolder, "Last.esp", 30_000L)
            val orderer = PluginTimestampOrderer(
                timestampIntervalMillis = 60_000L,
                clockMillis = { 1_000_000L }
            )

            val applied = orderer.apply(
                dataFolder = dataFolder,
                plugins = listOf(
                    plugin("Last.esp", priority = 30, enabled = true),
                    plugin("Disabled.esp", priority = 20, enabled = false),
                    plugin("First.esm", priority = 10, enabled = true)
                )
            )

            assertEquals(3, applied.appliedCount)
            assertTrue(first.lastModified() < disabled.lastModified())
            assertTrue(disabled.lastModified() < last.lastModified())
        }
    }

    @Test
    fun missingPluginFailsBeforeAnyTimestampChanges() {
        withDataFolder("timestamp-missing") { dataFolder ->
            val first = pluginFile(dataFolder, "First.esm", 10_000L)
            val original = first.lastModified()
            val orderer = PluginTimestampOrderer(clockMillis = { 1_000_000L })

            expectTimestampFailure {
                orderer.apply(
                    dataFolder = dataFolder,
                    plugins = listOf(
                        plugin("First.esm", priority = 1, enabled = true),
                        plugin("Missing.esp", priority = 2, enabled = true)
                    )
                )
            }

            assertEquals(original, first.lastModified())
        }
    }

    @Test
    fun duplicateCaseInsensitivePluginNamesFailBeforeMutation() {
        withDataFolder("timestamp-duplicates") { dataFolder ->
            val pluginFile = pluginFile(dataFolder, "Duplicate.esp", 10_000L)
            val original = pluginFile.lastModified()
            val orderer = PluginTimestampOrderer(clockMillis = { 1_000_000L })

            expectTimestampFailure {
                orderer.apply(
                    dataFolder = dataFolder,
                    plugins = listOf(
                        plugin("Duplicate.esp", priority = 1, enabled = true),
                        plugin("duplicate.ESP", priority = 2, enabled = false)
                    )
                )
            }

            assertEquals(original, pluginFile.lastModified())
        }
    }

    @Test
    fun midApplicationFailureRestoresChangedTimestamps() {
        withDataFolder("timestamp-rollback") { dataFolder ->
            val first = pluginFile(dataFolder, "First.esm", 10_000L)
            val second = pluginFile(dataFolder, "Second.esp", 20_000L)
            val firstOriginal = first.lastModified()
            val secondOriginal = second.lastModified()
            var writeCount = 0
            val orderer = PluginTimestampOrderer(
                clockMillis = { 1_000_000L },
                timestampWriter = { file, timestamp ->
                    writeCount++
                    if (writeCount == 2) {
                        false
                    } else {
                        file.setLastModified(timestamp)
                    }
                }
            )

            expectTimestampFailure {
                orderer.apply(
                    dataFolder = dataFolder,
                    plugins = listOf(
                        plugin("First.esm", priority = 1, enabled = true),
                        plugin("Second.esp", priority = 2, enabled = true)
                    )
                )
            }

            assertEquals(firstOriginal, first.lastModified())
            assertEquals(secondOriginal, second.lastModified())
        }
    }

    private fun withDataFolder(name: String, action: (File) -> Unit) {
        val folder = Files.createTempDirectory(name).toFile()
        try {
            action(folder)
        } finally {
            folder.deleteRecursively()
        }
    }

    private fun pluginFile(dataFolder: File, name: String, timestamp: Long): File {
        return File(dataFolder, name).apply {
            writeText(name)
            check(setLastModified(timestamp))
        }
    }

    private fun plugin(
        name: String,
        priority: Int,
        enabled: Boolean
    ): PluginEntry {
        return PluginEntry(
            normalizedPath = name.lowercase(),
            pluginName = name,
            sourceModId = "mod",
            sourceModName = "Mod",
            enabled = enabled,
            priority = priority,
            pluginType = if (name.endsWith(".esm", ignoreCase = true)) "ESM" else "ESP"
        )
    }

    private fun expectTimestampFailure(action: () -> Unit) {
        try {
            action()
            throw AssertionError("Expected PluginTimestampOrderException")
        } catch (_: PluginTimestampOrderException) {
        }
    }
}
