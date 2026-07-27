package com.shonkware.droidmodloader.engine.service

import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPreflightException
import com.shonkware.droidmodloader.engine.model.GameDeploymentConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DeploymentServiceTest {
    @Test
    fun `deployment config round trips through service`() {
        val fixture = fixture("config")
        val config = GameDeploymentConfig(
            gameId = "fallout_nv",
            displayName = "Fallout New Vegas",
            targetDataPath = "/games/fnv/Data",
            realDeployEnabled = true,
            targetRootPath = "/games/fnv"
        )

        fixture.service.saveGameDeploymentConfigs(listOf(config))

        assertEquals(config, fixture.service.getGameDeploymentConfig("fallout_nv"))
    }

    @Test
    fun `simulated target summary is stable when real deployment is unavailable`() {
        val fixture = fixture("summary")

        val summary = fixture.service.getDeploymentTargetDebugSummary("skyrim_le")

        assertTrue(summary.contains("mode=simulated"))
        assertTrue(summary.contains("deployment_manifest_skyrim_le_simulated_"))
        assertTrue(summary.contains("deployment_manifest_root_skyrim_le_root_simulated_"))
    }

    @Test
    fun `real target summary includes target type and game-aware validation`() {
        val fixture = fixture("validated-summary")
        val root = File(fixture.root, "Fallout New Vegas").apply { mkdirs() }
        val data = File(root, "Data").apply { mkdirs() }
        File(root, "FalloutNV.exe").writeText("fixture")
        File(data, "FalloutNV.esm").writeText("fixture")
        fixture.service.saveGameDeploymentConfigs(
            listOf(
                GameDeploymentConfig(
                    gameId = "fallout_nv",
                    displayName = "Fallout New Vegas",
                    targetDataPath = data.absolutePath,
                    realDeployEnabled = true,
                    targetRootPath = root.absolutePath
                )
            )
        )

        val summary = fixture.service.getDeploymentTargetDebugSummary("fallout_nv")

        assertTrue(summary.contains("Data Target Validation"))
        assertTrue(summary.contains("Target type: Data"))
        assertTrue(summary.contains("Game Root Target Validation"))
        assertTrue(summary.contains("Target type: Game Root"))
        assertTrue(summary.contains("Validation: valid"))
        assertTrue(summary.contains("Target Relationship Validation"))
    }

    @Test
    fun `invalid real target fails before deployment journal starts`() {
        val fixture = fixture("invalid-preflight")
        val data = File(fixture.root, "Wrong Data").apply { mkdirs() }
        File(data, "Fallout3.esm").writeText("fixture")
        fixture.service.saveGameDeploymentConfigs(
            listOf(
                GameDeploymentConfig(
                    gameId = "fallout_nv",
                    displayName = "Fallout New Vegas",
                    targetDataPath = data.absolutePath,
                    realDeployEnabled = true
                )
            )
        )

        try {
            fixture.service.deployForGame("fallout_nv")
            fail("Expected deployment preflight to reject the wrong game target.")
        } catch (expected: DeploymentPreflightException) {
            assertTrue(expected.result.issues.any { it.title.contains("another game") })
        }

        assertTrue(
            fixture.service.getDeploymentJournalDebugSummary("fallout_nv")
                .contains("No deploy journal found")
        )
    }

    @Test
    fun `empty winners create an empty deployment plan`() {
        val fixture = fixture("plan")

        val plan = fixture.service.buildDeploymentPlanForGame("oblivion")

        assertEquals(0, plan.totalOperationCount)
    }

    private fun fixture(name: String): Fixture {
        val root = Files.createTempDirectory("dml-deployment-service-$name").toFile()
        val stateDir = File(root, "state").apply { mkdirs() }
        return Fixture(
            root = root,
            service = DeploymentService(
                appFilesDir = File(root, "files").apply { mkdirs() },
                tempDir = File(root, "temp").apply { mkdirs() },
                stateFile = File(stateDir, "mods.json"),
                deploymentManifestFile = File(stateDir, "deployment_manifest.json"),
                deployRootDir = File(root, "deploy").apply { mkdirs() },
                gameConfigFile = File(stateDir, "game_config.json"),
                currentDataWinningRecords = { emptyList() },
                currentRootWinningRecords = { emptyList() }
            )
        )
    }

    private data class Fixture(
        val root: File,
        val service: DeploymentService
    )
}
