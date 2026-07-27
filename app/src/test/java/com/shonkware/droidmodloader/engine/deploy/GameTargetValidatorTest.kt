package com.shonkware.droidmodloader.engine.deploy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GameTargetValidatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val validator = GameTargetValidator()

    @Test
    fun supportedGamesMatchExpectedRootAndDataMarkers() {
        val fixtures = listOf(
            GameFixture("skyrim_le", "TESV.exe", "Skyrim.esm"),
            GameFixture("oblivion", "Oblivion.exe", "Oblivion.esm"),
            GameFixture("fallout_3", "Fallout3.exe", "Fallout3.esm"),
            GameFixture("fallout_nv", "FalloutNV.exe", "FalloutNV.esm"),
            GameFixture("ttw", "FalloutNV.exe", "FalloutNV.esm")
        )

        fixtures.forEach { fixture ->
            val root = temporaryFolder.newFolder("${fixture.gameId}-root")
            val data = File(root, "Data").apply { mkdirs() }
            File(root, fixture.executable).writeText("fixture")
            File(data, fixture.master).writeText("fixture")

            val rootResult = validator.validateTarget(
                gameId = fixture.gameId,
                targetType = GameTargetType.GAME_ROOT,
                path = root.absolutePath
            )
            val dataResult = validator.validateTarget(
                gameId = fixture.gameId,
                targetType = GameTargetType.DATA,
                path = data.absolutePath
            )

            assertTrue("${fixture.gameId} root: ${rootResult.findings}", rootResult.canDeploy)
            assertTrue("${fixture.gameId} data: ${dataResult.findings}", dataResult.canDeploy)
            assertTrue(
                validator.validateRelationship(dataResult, rootResult)
                    .none { it.severity == GameTargetValidationSeverity.ERROR }
            )
        }
    }

    @Test
    fun markerMatchingIsCaseInsensitive() {
        val root = temporaryFolder.newFolder("case-root")
        val data = File(root, "DATA").apply { mkdirs() }
        File(root, "fAlLoUtNv.ExE").writeText("fixture")
        File(data, "fAlLoUtNv.EsM").writeText("fixture")

        assertTrue(
            validator.validateTarget(
                gameId = "fallout_nv",
                targetType = GameTargetType.GAME_ROOT,
                path = root.absolutePath
            ).canDeploy
        )
        assertTrue(
            validator.validateTarget(
                gameId = "fallout_nv",
                targetType = GameTargetType.DATA,
                path = data.absolutePath
            ).canDeploy
        )
    }

    @Test
    fun markerNamedDirectoriesDoNotSatisfyRequiredFiles() {
        val root = temporaryFolder.newFolder("marker-directory-root")
        val data = File(root, "Data").apply { mkdirs() }
        File(root, "FalloutNV.exe").mkdirs()
        File(data, "FalloutNV.esm").mkdirs()

        val rootResult = validator.validateTarget(
            gameId = "fallout_nv",
            targetType = GameTargetType.GAME_ROOT,
            path = root.absolutePath
        )
        val dataResult = validator.validateTarget(
            gameId = "fallout_nv",
            targetType = GameTargetType.DATA,
            path = data.absolutePath
        )

        assertFalse(rootResult.canDeploy)
        assertTrue(rootResult.findings.any { it.code == "GAME_MARKERS_MISSING" })
        assertFalse(dataResult.canDeploy)
        assertTrue(dataResult.findings.any { it.code == "GAME_MARKERS_MISSING" })
    }

    @Test
    fun wrongGameDataTargetIsBlocked() {
        val data = temporaryFolder.newFolder("Data")
        File(data, "Fallout3.esm").writeText("fixture")

        val result = validator.validateTarget(
            gameId = "fallout_nv",
            targetType = GameTargetType.DATA,
            path = data.absolutePath
        )

        assertFalse(result.canDeploy)
        assertTrue(result.findings.any { it.code == "WRONG_GAME_TARGET" })
    }

    @Test
    fun dataFolderSelectedAsRootIsBlocked() {
        val data = temporaryFolder.newFolder("data-as-root")
        File(data, "FalloutNV.esm").writeText("fixture")

        val result = validator.validateTarget(
            gameId = "fallout_nv",
            targetType = GameTargetType.GAME_ROOT,
            path = data.absolutePath
        )

        assertFalse(result.canDeploy)
        assertTrue(result.findings.any { it.code == "TARGET_TYPE_MISMATCH" })
    }

    @Test
    fun gameRootSelectedAsDataIsBlocked() {
        val root = temporaryFolder.newFolder("root-as-data")
        File(root, "FalloutNV.exe").writeText("fixture")

        val result = validator.validateTarget(
            gameId = "fallout_nv",
            targetType = GameTargetType.DATA,
            path = root.absolutePath
        )

        assertFalse(result.canDeploy)
        assertTrue(result.findings.any { it.code == "TARGET_TYPE_MISMATCH" })
    }

    @Test
    fun mismatchedDataAndRootInstallationsAreBlocked() {
        val firstRoot = temporaryFolder.newFolder("first-root")
        val secondRoot = temporaryFolder.newFolder("second-root")
        val data = File(firstRoot, "Data").apply { mkdirs() }
        File(data, "FalloutNV.esm").writeText("fixture")
        File(secondRoot, "FalloutNV.exe").writeText("fixture")

        val findings = validator.validateRelationship(
            dataResult = validator.validateTarget(
                gameId = "fallout_nv",
                targetType = GameTargetType.DATA,
                path = data.absolutePath
            ),
            rootResult = validator.validateTarget(
                gameId = "fallout_nv",
                targetType = GameTargetType.GAME_ROOT,
                path = secondRoot.absolutePath
            )
        )

        assertTrue(findings.any { it.code == "TARGETS_DIFFERENT_INSTALLATIONS" })
    }

    @Test
    fun unusualDataFolderNameWarnsButAllowsDeployment() {
        val data = temporaryFolder.newFolder("GameData")
        File(data, "Oblivion.esm").writeText("fixture")

        val result = validator.validateTarget(
            gameId = "oblivion",
            targetType = GameTargetType.DATA,
            path = data.absolutePath
        )

        assertTrue(result.canDeploy)
        assertEquals("warning", result.statusLabel)
        assertTrue(result.findings.any { it.code == "UNUSUAL_DATA_FOLDER_NAME" })
    }

    @Test
    fun ttwAcceptsCleanFalloutNewVegasTargetBeforeInitialDeployment() {
        val root = temporaryFolder.newFolder("ttw-root")
        val data = File(root, "Data").apply { mkdirs() }
        File(root, "FalloutNV.exe").writeText("fixture")
        File(data, "FalloutNV.esm").writeText("fixture")

        val result = validator.validateTarget(
            gameId = "ttw",
            targetType = GameTargetType.DATA,
            path = data.absolutePath
        )

        assertTrue(result.canDeploy)
        assertTrue(result.findings.any { it.code == "TTW_MARKER_NOT_YET_PRESENT" })
    }

    @Test
    fun unknownGameDefinitionBlocksDeployment() {
        val data = temporaryFolder.newFolder("unknown-data")

        val result = validator.validateTarget(
            gameId = "unsupported_game",
            targetType = GameTargetType.DATA,
            path = data.absolutePath
        )

        assertFalse(result.canDeploy)
        assertTrue(result.findings.any { it.code == "UNKNOWN_GAME" })
    }

    @Test
    fun filesystemRootIsRejectedAsTooBroad() {
        val result = validator.validateTarget(
            gameId = "fallout_nv",
            targetType = GameTargetType.GAME_ROOT,
            path = File.listRoots().first().absolutePath
        )

        assertFalse(result.canDeploy)
        assertTrue(result.findings.any { it.code == "TARGET_PATH_TOO_BROAD" })
    }

    private data class GameFixture(
        val gameId: String,
        val executable: String,
        val master: String
    )
}
