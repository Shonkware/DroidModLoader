package com.shonkware.droidmodloader.engine.data

import com.shonkware.droidmodloader.engine.plugins.PluginOutputContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PluginOutputRepositoryTest {
    @Test
    fun replaceOutputsWritesBothTextFiles() {
        withTempFolder("plugin-output-both") { root ->
            val pluginsFile = File(root, "state/plugins.txt")
            val loadorderFile = File(root, "state/loadorder.txt")
            val repository = PluginOutputRepository(pluginsFile, loadorderFile)

            val paths = repository.replaceOutputs(
                PluginOutputContent(
                    pluginsTxt = "Skyrim.esm",
                    loadorderTxt = "Skyrim.esm\nDisabled.esp"
                )
            )

            assertEquals("Skyrim.esm", pluginsFile.readText())
            assertEquals("Skyrim.esm\nDisabled.esp", loadorderFile.readText())
            assertEquals(pluginsFile.absolutePath, paths.pluginsTxtPath)
            assertEquals(loadorderFile.absolutePath, paths.loadorderTxtPath)
        }
    }

    @Test
    fun replaceOutputsRemovesStaleLoadorderWhenGameDoesNotUseIt() {
        withTempFolder("plugin-output-remove") { root ->
            val stateFolder = File(root, "state").apply { mkdirs() }
            val pluginsFile = File(stateFolder, "plugins.txt")
            val loadorderFile = File(stateFolder, "loadorder.txt").apply {
                writeText("stale")
            }
            val repository = PluginOutputRepository(pluginsFile, loadorderFile)

            val paths = repository.replaceOutputs(
                PluginOutputContent(
                    pluginsTxt = "FalloutNV.esm",
                    loadorderTxt = null
                )
            )

            assertEquals("FalloutNV.esm", pluginsFile.readText())
            assertFalse(loadorderFile.exists())
            assertEquals(null, paths.loadorderTxtPath)
        }
    }

    private fun withTempFolder(name: String, action: (File) -> Unit) {
        val folder = Files.createTempDirectory(name).toFile()
        try {
            action(folder)
        } finally {
            folder.deleteRecursively()
        }
    }
}
