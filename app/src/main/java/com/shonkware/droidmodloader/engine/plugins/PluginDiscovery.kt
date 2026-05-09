package com.shonkware.droidmodloader.engine.plugins

import com.shonkware.droidmodloader.engine.model.Mod
import com.shonkware.droidmodloader.engine.model.PluginEntry
import java.io.File

class PluginDiscovery {

    fun discoverPlugins(mods: List<Mod>): List<PluginEntry> {
        val results = mutableListOf<PluginEntry>()
        var nextPriority = 10

        val sortedMods = mods
            .filter { it.enabled }
            .sortedBy { it.priority }

        for (mod in sortedMods) {
            val modDir = File(mod.installPath)
            if (!modDir.exists() || !modDir.isDirectory) continue

            val pluginFiles = modDir.walkTopDown()
                .filter { it.isFile }
                .filter { isPluginFile(it.name) }
                .sortedBy { it.name.lowercase() }
                .toList()

            for (pluginFile in pluginFiles) {
                results.add(
                    PluginEntry(
                        pluginName = pluginFile.name,
                        sourceModId = mod.id,
                        sourceModName = mod.name,
                        enabled = true,
                        priority = nextPriority,
                        pluginType = detectPluginType(pluginFile.name)
                    )
                )
                nextPriority += 10
            }
        }

        return results
    }

    private fun isPluginFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".esp") || lower.endsWith(".esm") || lower.endsWith(".esl")
    }

    private fun detectPluginType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".esm") -> "ESM"
            lower.endsWith(".esl") -> "ESL"
            else -> "ESP"
        }
    }
}