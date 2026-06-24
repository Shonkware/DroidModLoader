package com.shonkware.droidmodloader.engine.service

import com.shonkware.droidmodloader.engine.index.ModContentCategory
import com.shonkware.droidmodloader.engine.index.ModContentEntry
import com.shonkware.droidmodloader.engine.index.ModContentIndex
import com.shonkware.droidmodloader.engine.index.ModFilePreviewStatus
import com.shonkware.droidmodloader.engine.model.DeployScope
import com.shonkware.droidmodloader.engine.model.FileRecord
import com.shonkware.droidmodloader.engine.model.Mod
import com.shonkware.droidmodloader.engine.model.ModType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModInspectionServiceTest {
    @Test
    fun `empty profile builds an empty resolved graph`() {
        val fixture = fixture("graph")

        val graph = fixture.service.buildCurrentResolvedDataGraph()

        assertEquals(0, graph.profileModCount)
        assertEquals(0, graph.enabledModCount)
        assertTrue(graph.paths.isEmpty())
    }

    @Test
    fun `preview reports winning deployable file and folder summary`() {
        val mod = Mod(
            id = "example-mod",
            name = "Example Mod",
            installPath = "/mods/example",
            enabled = true,
            priority = 100,
            modType = ModType.LOOSE
        )
        val index = ModContentIndex(
            modId = mod.id,
            modName = mod.name,
            entries = listOf(
                ModContentEntry(
                    originalPath = "textures/example/test.dds",
                    normalizedPath = "textures/example/test.dds",
                    category = ModContentCategory.GAME_FILE,
                    reason = "Deployable Data file",
                    isDeployable = true,
                    deployScope = DeployScope.DATA
                )
            )
        )
        val winner = FileRecord(
            normalizedPath = "textures/example/test.dds",
            winningModId = mod.id,
            winningModName = mod.name,
            sourceFilePath = "/mods/example/textures/example/test.dds",
            hash = "hash"
        )
        val fixture = fixture(
            name = "preview",
            mods = listOf(mod),
            indexes = mapOf(mod.id to index),
            dataWinners = listOf(winner)
        )

        val preview = fixture.service.buildModFilePreview(mod)

        assertEquals(ModFilePreviewStatus.WINNING, preview.entries.single().status)
        assertEquals("textures/", preview.folderSummaries.single().displayName)
        assertEquals(1, preview.folderSummaries.single().dataFileCount)
        assertEquals(1, preview.folderSummaries.single().winningCount)
    }

    @Test
    fun `rebuilding baseline records the current simulated target`() {
        val fixture = fixture("baseline")

        assertFalse(fixture.service.hasDataBaseline("skyrim_le"))

        val snapshot = fixture.service.rebuildDataBaseline("skyrim_le")

        assertTrue(fixture.service.hasDataBaseline("skyrim_le"))
        assertEquals("skyrim_le", snapshot.gameId)
        assertEquals(fixture.deployRoot.absolutePath, snapshot.targetDescription)
        assertTrue(snapshot.files.isEmpty())
    }

    private fun fixture(
        name: String,
        mods: List<Mod> = emptyList(),
        indexes: Map<String, ModContentIndex> = emptyMap(),
        dataWinners: List<FileRecord> = emptyList(),
        rootWinners: List<FileRecord> = emptyList()
    ): Fixture {
        val root = Files.createTempDirectory("dml-mod-inspection-$name").toFile()
        val stateDir = File(root, "state").apply { mkdirs() }
        val deployRoot = File(root, "deploy").apply { mkdirs() }
        val service = ModInspectionService(
            modFileIndexDir = File(stateDir, "mod_file_indexes"),
            deploymentManifestFile = File(stateDir, "deployment_manifest.json"),
            deployRootDir = deployRoot,
            currentMods = { mods },
            indexContent = { mod ->
                indexes[mod.id] ?: ModContentIndex(mod.id, mod.name, emptyList())
            },
            installedRecords = { emptyMap() },
            dataWinningRecords = { dataWinners },
            rootWinningRecords = { rootWinners },
            deploymentConfig = { null },
            isValidTargetPath = { false },
            effectiveManifestFile = { gameId ->
                File(stateDir, "deployment_manifest_${gameId}.json")
            },
            targetScopedFileName = { prefix, gameId -> "${prefix}_${gameId}.json" }
        )
        return Fixture(service, deployRoot)
    }

    private data class Fixture(
        val service: ModInspectionService,
        val deployRoot: File
    )
}
