package com.shonkware.droidmodloader.engine.plugins

import com.shonkware.droidmodloader.engine.data.PluginOutputRepository
import com.shonkware.droidmodloader.engine.model.PluginEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PluginConfigurationApplierTest {
    private val rules = GamePluginLoadOrderRules()

    @Test
    fun emptyPluginListPreservesPriorOutputs() {
        withTempFolder("plugin-apply-empty") { root ->
            val stateFolder = File(root, "state").apply { mkdirs() }
            val pluginsFile = File(stateFolder, "plugins.txt").apply {
                writeText("previous-plugins")
            }
            val loadorderFile = File(stateFolder, "loadorder.txt").apply {
                writeText("previous-order")
            }
            val applier = createApplier(pluginsFile, loadorderFile)

            expectApplyFailure {
                applier.apply(
                    rule = rules.require("skyrim_le"),
                    plugins = emptyList(),
                    timestampDataFolder = null
                )
            }

            assertEquals("previous-plugins", pluginsFile.readText())
            assertEquals("previous-order", loadorderFile.readText())
        }
    }

    @Test
    fun skyrimWritesTextOutputsWithoutTimestampTarget() {
        withTempFolder("plugin-apply-skyrim") { root ->
            val pluginsFile = File(root, "state/plugins.txt")
            val loadorderFile = File(root, "state/loadorder.txt")
            val applier = createApplier(pluginsFile, loadorderFile)

            val result = applier.apply(
                rule = rules.require("skyrim_le"),
                plugins = listOf(
                    plugin("Late.esp", priority = 3, enabled = true),
                    plugin("Disabled.esp", priority = 2, enabled = false),
                    plugin("Skyrim.esm", priority = 1, enabled = true)
                ),
                timestampDataFolder = null
            )

            assertEquals(PluginLoadOrderMechanism.TEXT_FILES, result.mechanism)
            assertEquals("Skyrim.esm\nLate.esp", pluginsFile.readText())
            assertEquals("Skyrim.esm\nDisabled.esp\nLate.esp", loadorderFile.readText())
            assertEquals(0, result.timestampedPluginCount)
            assertNull(result.timestampDataFolderPath)
        }
    }

    @Test
    fun timestampGameOrdersCompleteListAndRemovesStaleLoadorderOutput() {
        withTempFolder("plugin-apply-fnv") { root ->
            val stateFolder = File(root, "state").apply { mkdirs() }
            val dataFolder = File(root, "Data").apply { mkdirs() }
            val pluginsFile = File(stateFolder, "plugins.txt")
            val loadorderFile = File(stateFolder, "loadorder.txt").apply {
                writeText("stale-order")
            }
            val first = pluginFile(dataFolder, "FalloutNV.esm", 30_000L)
            val disabled = pluginFile(dataFolder, "Disabled.esp", 20_000L)
            val last = pluginFile(dataFolder, "Late.esp", 10_000L)
            val applier = createApplier(
                pluginsFile = pluginsFile,
                loadorderFile = loadorderFile,
                clockMillis = { 1_000_000L }
            )

            val result = applier.apply(
                rule = rules.require("fallout_nv"),
                plugins = listOf(
                    plugin("Late.esp", priority = 3, enabled = true),
                    plugin("Disabled.esp", priority = 2, enabled = false),
                    plugin("FalloutNV.esm", priority = 1, enabled = true)
                ),
                timestampDataFolder = dataFolder
            )

            assertEquals(PluginLoadOrderMechanism.FILE_TIMESTAMPS, result.mechanism)
            assertEquals("FalloutNV.esm\nLate.esp", pluginsFile.readText())
            assertFalse(loadorderFile.exists())
            assertTrue(first.lastModified() < disabled.lastModified())
            assertTrue(disabled.lastModified() < last.lastModified())
            assertEquals(3, result.timestampedPluginCount)
            assertEquals(dataFolder.absolutePath, result.timestampDataFolderPath)
        }
    }

    @Test
    fun missingTimestampTargetPreservesPriorOutputsAndTimestamps() {
        withTempFolder("plugin-apply-missing") { root ->
            val stateFolder = File(root, "state").apply { mkdirs() }
            val dataFolder = File(root, "Data").apply { mkdirs() }
            val pluginsFile = File(stateFolder, "plugins.txt").apply {
                writeText("previous-plugins")
            }
            val loadorderFile = File(stateFolder, "loadorder.txt").apply {
                writeText("previous-order")
            }
            val first = pluginFile(dataFolder, "FalloutNV.esm", 10_000L)
            val firstOriginal = first.lastModified()
            val applier = createApplier(pluginsFile, loadorderFile)

            expectApplyFailure {
                applier.apply(
                    rule = rules.require("fallout_nv"),
                    plugins = listOf(
                        plugin("FalloutNV.esm", priority = 1, enabled = true),
                        plugin("Missing.esp", priority = 2, enabled = true)
                    ),
                    timestampDataFolder = dataFolder
                )
            }

            assertEquals("previous-plugins", pluginsFile.readText())
            assertEquals("previous-order", loadorderFile.readText())
            assertEquals(firstOriginal, first.lastModified())
        }
    }

    @Test
    fun outputFailureRestoresAppliedTimestamps() {
        withTempFolder("plugin-apply-output-failure") { root ->
            val dataFolder = File(root, "Data").apply { mkdirs() }
            val first = pluginFile(dataFolder, "FalloutNV.esm", 10_000L)
            val second = pluginFile(dataFolder, "Second.esp", 20_000L)
            val firstOriginal = first.lastModified()
            val secondOriginal = second.lastModified()
            val blockedParent = File(root, "blocked-parent").apply {
                writeText("not a directory")
            }
            val applier = createApplier(
                pluginsFile = File(blockedParent, "plugins.txt"),
                loadorderFile = File(blockedParent, "loadorder.txt"),
                clockMillis = { 1_000_000L }
            )

            expectApplyFailure {
                applier.apply(
                    rule = rules.require("fallout_nv"),
                    plugins = listOf(
                        plugin("FalloutNV.esm", priority = 1, enabled = true),
                        plugin("Second.esp", priority = 2, enabled = true)
                    ),
                    timestampDataFolder = dataFolder
                )
            }

            assertEquals(firstOriginal, first.lastModified())
            assertEquals(secondOriginal, second.lastModified())
        }
    }

    private fun createApplier(
        pluginsFile: File,
        loadorderFile: File,
        clockMillis: () -> Long = { System.currentTimeMillis() }
    ): PluginConfigurationApplier {
        return PluginConfigurationApplier(
            outputBuilder = PluginOutputBuilder(),
            outputRepository = PluginOutputRepository(pluginsFile, loadorderFile),
            timestampOrderer = PluginTimestampOrderer(clockMillis = clockMillis)
        )
    }

    private fun withTempFolder(name: String, action: (File) -> Unit) {
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

    private fun expectApplyFailure(action: () -> Unit) {
        try {
            action()
            throw AssertionError("Expected plugin application failure")
        } catch (_: IllegalStateException) {
        }
    }
}
