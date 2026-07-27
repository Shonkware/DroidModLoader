package com.shonkware.droidmodloader.engine.deploy

import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPlan
import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPlanOperation
import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPlanOperationType
import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPlanScope
import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPreflightChecker
import com.shonkware.droidmodloader.engine.deploy.plan.DeploymentPreflightSeverity
import com.shonkware.droidmodloader.engine.deploy.plan.ScopedDeploymentPlan
import com.shonkware.droidmodloader.engine.model.GameDeploymentConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DeploymentPreflightCheckerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun gameAwareDataPathPassesTargetPreflight() {
        val root = temporaryFolder.newFolder("fnv-root")
        val dataDir = File(root, "Data").apply { mkdirs() }
        File(dataDir, "FalloutNV.esm").writeText("fixture")

        val result = DeploymentPreflightChecker().check(
            config = config(targetDataPath = dataDir.canonicalPath),
            plan = emptyPlan()
        )

        assertTrue(result.canDeploy)
        assertTrue(result.issues.any { it.title == "Data target is available." })
    }

    @Test
    fun wrongGameDataPathBlocksTargetPreflight() {
        val dataDir = temporaryFolder.newFolder("wrong-game-data")
        File(dataDir, "Fallout3.esm").writeText("fixture")

        val result = DeploymentPreflightChecker().check(
            config = config(targetDataPath = dataDir.canonicalPath),
            plan = emptyPlan()
        )

        assertFalse(result.canDeploy)
        assertTrue(result.issues.any { it.title.contains("another game") })
    }

    @Test
    fun mismatchedDataAndRootTargetsBlockPreflight() {
        val dataRoot = temporaryFolder.newFolder("data-install")
        val rootTarget = temporaryFolder.newFolder("root-install")
        val dataDir = File(dataRoot, "Data").apply { mkdirs() }
        File(dataDir, "FalloutNV.esm").writeText("fixture")
        File(rootTarget, "FalloutNV.exe").writeText("fixture")

        val result = DeploymentPreflightChecker().check(
            config = config(
                targetDataPath = dataDir.canonicalPath,
                targetRootPath = rootTarget.canonicalPath
            ),
            plan = emptyPlan()
        )

        assertFalse(result.canDeploy)
        assertTrue(
            result.issues.any {
                it.title == "Data and Game Root targets do not belong to the same installation."
            }
        )
    }

    @Test
    fun simulatedDeploymentDoesNotRequireGameMarkers() {
        val result = DeploymentPreflightChecker().check(
            config = config(
                targetDataPath = "",
                realDeployEnabled = false
            ),
            plan = emptyPlan()
        )

        assertTrue(result.canDeploy)
        assertTrue(result.issues.any { it.title == "Real deploy is disabled." })
    }

    @Test
    fun legacyDataSelectionRequiresReselectionBeforeDeploy() {
        val result = DeploymentPreflightChecker().check(
            config = config(
                targetDataPath = "",
                dataPathReselectionRequired = true
            ),
            plan = emptyPlan()
        )

        assertFalse(result.canDeploy)
        assertTrue(
            result.issues.any {
                it.severity == DeploymentPreflightSeverity.ERROR &&
                    it.title == "Data target must be reselected."
            }
        )
    }

    @Test
    fun rootOperationsRequireWritableDirectRootPath() {
        val dataDir = temporaryFolder.newFolder("Data")
        File(dataDir, "FalloutNV.esm").writeText("fixture")
        val plan = ScopedDeploymentPlan(
            dataPlan = DeploymentPlan(DeploymentPlanScope.DATA, emptyList()),
            rootPlan = DeploymentPlan(
                scope = DeploymentPlanScope.GAME_ROOT,
                operations = listOf(
                    DeploymentPlanOperation(
                        type = DeploymentPlanOperationType.REMOVE,
                        normalizedPath = "nvse_loader.exe",
                        newRecord = null,
                        oldRecord = null,
                        winningModName = null,
                        sourceSizeBytes = null,
                        reason = "test"
                    )
                )
            )
        )

        val result = DeploymentPreflightChecker().check(
            config = config(
                targetDataPath = dataDir.canonicalPath,
                targetRootPath = ""
            ),
            plan = plan
        )

        assertFalse(result.canDeploy)
        assertTrue(result.issues.any { it.title == "Game Root target is not selected." })
    }

    private fun config(
        targetDataPath: String,
        targetRootPath: String = "",
        realDeployEnabled: Boolean = true,
        dataPathReselectionRequired: Boolean = false,
        rootPathReselectionRequired: Boolean = false
    ) = GameDeploymentConfig(
        gameId = "fallout_nv",
        displayName = "Fallout New Vegas",
        targetDataPath = targetDataPath,
        realDeployEnabled = realDeployEnabled,
        targetRootPath = targetRootPath,
        dataPathReselectionRequired = dataPathReselectionRequired,
        rootPathReselectionRequired = rootPathReselectionRequired
    )

    private fun emptyPlan() = ScopedDeploymentPlan(
        dataPlan = DeploymentPlan(DeploymentPlanScope.DATA, emptyList()),
        rootPlan = DeploymentPlan(DeploymentPlanScope.GAME_ROOT, emptyList())
    )
}
