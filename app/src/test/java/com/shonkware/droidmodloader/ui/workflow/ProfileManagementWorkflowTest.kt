package com.shonkware.droidmodloader.ui.workflow

import com.shonkware.droidmodloader.engine.model.AppSetupState
import com.shonkware.droidmodloader.engine.model.GameProfile
import com.shonkware.droidmodloader.engine.profile.ProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProfileManagementWorkflowTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun completeFirstSetupCreatesDefaultProfileAndActivatesIt() {
        val repository = createRepository("first-setup")
        var appliedProfile: GameProfile? = null
        var savedConfigCount = 0
        var refreshCount = 0
        val statuses = mutableListOf<String>()

        val workflow = createWorkflow(
            repository = repository,
            currentTimeMillis = { 1234L },
            firstSetupInputProvider = {
                FirstSetupInput(
                    profileNameText = "   ",
                    gameId = "skyrim_le",
                    targetDataPath = "/games/skyrim/Data",
                    realDeployEnabled = true
                )
            },
            applyFirstSetupUiState = { _, profile ->
                appliedProfile = profile
            },
            saveSelectedGameConfigFromUi = {
                savedConfigCount++
            },
            refreshDashboard = {
                refreshCount++
            },
            updateLastOperationStatus = {
                statuses.add(it)
            }
        )

        workflow.completeFirstSetup()

        val profiles = repository.loadProfiles()
        assertEquals(1, profiles.size)
        assertEquals("skyrim_le_1234", profiles.single().profileId)
        assertEquals("Default", profiles.single().profileName)
        assertEquals("/games/skyrim/Data", profiles.single().targetDataPath)
        assertTrue(profiles.single().realDeployEnabled)
        assertEquals(profiles.single(), appliedProfile)
        assertEquals(
            AppSetupState(
                setupComplete = true,
                activeProfileId = "skyrim_le_1234"
            ),
            repository.loadSetupState()
        )
        assertEquals(1, savedConfigCount)
        assertEquals(1, refreshCount)
        assertEquals(listOf("Setup complete."), statuses)
    }

    @Test
    fun createAdditionalProfileUsesFallbackNameAndDirectPath() {
        val repository = createRepository("additional-profile")
        var appliedProfile: GameProfile? = null
        val events = mutableListOf<String>()

        val workflow = createWorkflow(
            repository = repository,
            currentTimeMillis = { 2000L },
            additionalProfileInputProvider = {
                AdditionalProfileInput(
                    profileNameText = "",
                    gameId = "fallout_nv",
                    targetDataPath = "/games/falloutnv/Data",
                    realDeployEnabled = false
                )
            },
            applyCreatedProfileUiState = { _, profile ->
                appliedProfile = profile
                events += "apply"
            },
            saveSelectedGameConfigFromUi = {
                events += "save-config"
            },
            recoverActiveProfile = {
                events += "recover"
            },
            syncPluginsFromCurrentState = {
                events += "sync"
            },
            refreshDashboard = {
                events += "refresh"
            }
        )

        workflow.createAdditionalProfile()

        val profile = repository.loadProfiles().single()
        assertEquals("fallout_nv_2000", profile.profileId)
        assertEquals("Fallout New Vegas Profile", profile.profileName)
        assertEquals("/games/falloutnv/Data", profile.targetDataPath)
        assertFalse(profile.realDeployEnabled)
        assertEquals(profile, appliedProfile)
        assertEquals(
            "fallout_nv_2000",
            repository.loadSetupState().activeProfileId
        )
        assertEquals(
            listOf(
                "apply",
                "save-config",
                "recover",
                "sync",
                "refresh"
            ),
            events
        )
    }

    @Test
    fun switchActiveProfileSavesCurrentDashboardStateBeforeActivatingTarget() {
        val repository = createRepository("switch-profile")
        val currentProfile = profile(
            id = "current",
            name = "Current",
            gameId = "skyrim_le"
        )
        val targetProfile = profile(
            id = "target",
            name = "Target",
            gameId = "fallout_nv"
        )
        repository.saveProfiles(listOf(currentProfile, targetProfile))
        repository.saveSetupState(
            AppSetupState(
                setupComplete = true,
                activeProfileId = currentProfile.profileId
            )
        )

        var activeProfileId: String? = currentProfile.profileId
        var switchedProfile: GameProfile? = null
        val events = mutableListOf<String>()

        val workflow = createWorkflow(
            repository = repository,
            activeProfileIdProvider = { activeProfileId },
            dashboardProfileInputProvider = {
                DashboardProfileInput(
                    targetPathText = " /games/skyrim/Data ",
                    rootTargetPathText = " /games/skyrim ",
                    realDeployEnabled = true,
                    dataPathReselectionRequired = false,
                    rootPathReselectionRequired = false
                )
            },
            applySwitchedProfileUiState = { profile ->
                switchedProfile = profile
                activeProfileId = profile.profileId
                events += "apply"
            },
            loadSelectedGameConfigIntoUi = {
                events += "load-config"
            },
            recoverActiveProfile = {
                events += "recover"
            },
            syncPluginsFromCurrentState = {
                events += "sync"
            },
            refreshDashboard = {
                events += "refresh"
            }
        )

        workflow.switchActiveProfile(targetProfile.profileId)

        val savedProfiles = repository.loadProfiles()
        val savedCurrent = savedProfiles.first {
            it.profileId == currentProfile.profileId
        }
        assertEquals("/games/skyrim/Data", savedCurrent.targetDataPath)
        assertEquals("/games/skyrim", savedCurrent.targetRootPath)
        assertTrue(savedCurrent.realDeployEnabled)
        assertEquals(targetProfile, switchedProfile)
        assertEquals(
            targetProfile.profileId,
            repository.loadSetupState().activeProfileId
        )
        assertEquals(
            listOf(
                "apply",
                "load-config",
                "recover",
                "sync",
                "refresh"
            ),
            events
        )
    }

    @Test
    fun deleteActiveProfileSelectsFallbackAndAppliesBothUiUpdates() {
        val repository = createRepository("delete-profile")
        val firstProfile = profile("first", "First", "skyrim_le")
        val secondProfile = profile("second", "Second", "fallout_nv")
        repository.saveProfiles(listOf(firstProfile, secondProfile))
        repository.saveSetupState(
            AppSetupState(
                setupComplete = true,
                activeProfileId = firstProfile.profileId
            )
        )

        val asyncSelections = mutableListOf<GameProfile?>()
        val blockingSelections = mutableListOf<GameProfile?>()
        val events = mutableListOf<String>()

        val workflow = createWorkflow(
            repository = repository,
            activeProfileIdProvider = { firstProfile.profileId },
            applyDeletedProfileUiStateAsync = { _, active ->
                asyncSelections.add(active)
                events += "apply-async"
            },
            applyDeletedProfileUiStateBlocking = { _, active ->
                blockingSelections.add(active)
                events += "apply-blocking"
            },
            loadSelectedGameConfigIntoUi = {
                events += "load-config"
            },
            recoverActiveProfile = {
                events += "recover"
            },
            syncPluginsFromCurrentState = {
                events += "sync"
            },
            refreshDashboard = {
                events += "refresh"
            }
        )

        workflow.deleteProfile(firstProfile.profileId)

        assertEquals(listOf(secondProfile), repository.loadProfiles())
        assertEquals(
            secondProfile.profileId,
            repository.loadSetupState().activeProfileId
        )
        assertEquals(listOf(secondProfile), asyncSelections)
        assertEquals(listOf(secondProfile), blockingSelections)
        assertEquals(
            listOf(
                "apply-async",
                "apply-blocking",
                "load-config",
                "recover",
                "sync",
                "refresh"
            ),
            events
        )
    }

    @Test
    fun deletingInactiveProfileDoesNotRecoverActiveProfile() {
        val repository = createRepository("delete-inactive-profile")
        val activeProfile = profile("active", "Active", "skyrim_le")
        val inactiveProfile = profile("inactive", "Inactive", "fallout_nv")
        repository.saveProfiles(listOf(activeProfile, inactiveProfile))
        repository.saveSetupState(
            AppSetupState(
                setupComplete = true,
                activeProfileId = activeProfile.profileId
            )
        )

        var recoveryCount = 0
        var syncCount = 0
        var refreshCount = 0

        val workflow = createWorkflow(
            repository = repository,
            activeProfileIdProvider = { activeProfile.profileId },
            recoverActiveProfile = {
                recoveryCount++
            },
            syncPluginsFromCurrentState = {
                syncCount++
            },
            refreshDashboard = {
                refreshCount++
            }
        )

        workflow.deleteProfile(inactiveProfile.profileId)

        assertEquals(listOf(activeProfile), repository.loadProfiles())
        assertEquals(
            activeProfile.profileId,
            repository.loadSetupState().activeProfileId
        )
        assertEquals(0, recoveryCount)
        assertEquals(0, syncCount)
        assertEquals(1, refreshCount)
    }

    private fun createRepository(name: String): ProfileRepository {
        val directory = temporaryFolder.newFolder(name)
        return ProfileRepository(
            profilesFile = File(directory, "profiles.json"),
            setupStateFile = File(directory, "app_setup.json")
        )
    }

    private fun createWorkflow(
        repository: ProfileRepository,
        currentTimeMillis: () -> Long = { 1L },
        firstSetupInputProvider: () -> FirstSetupInput = {
            FirstSetupInput("Default", "skyrim_le", "", false)
        },
        additionalProfileInputProvider: () -> AdditionalProfileInput = {
            AdditionalProfileInput(
                "New Profile",
                "skyrim_le",
                "",
                false
            )
        },
        activeProfileIdProvider: () -> String? = { null },
        dashboardProfileInputProvider: () -> DashboardProfileInput = {
            DashboardProfileInput(
                targetPathText = "",
                rootTargetPathText = "",
                realDeployEnabled = false,
                dataPathReselectionRequired = false,
                rootPathReselectionRequired = false
            )
        },
        applyFirstSetupUiState: (List<GameProfile>, GameProfile) -> Unit = { _, _ -> },
        applyCreatedProfileUiState: (List<GameProfile>, GameProfile) -> Unit = { _, _ -> },
        applySwitchedProfileUiState: (GameProfile) -> Unit = {},
        applySavedProfileUiState: (List<GameProfile>, GameProfile) -> Unit = { _, _ -> },
        applyDeletedProfileUiStateAsync: (List<GameProfile>, GameProfile?) -> Unit = { _, _ -> },
        applyDeletedProfileUiStateBlocking: (List<GameProfile>, GameProfile?) -> Unit = { _, _ -> },
        saveSelectedGameConfigFromUi: () -> Unit = {},
        loadSelectedGameConfigIntoUi: () -> Unit = {},
        recoverActiveProfile: () -> Unit = {},
        syncPluginsFromCurrentState: () -> Unit = {},
        refreshDashboard: () -> Unit = {},
        updateLastOperationStatus: (String) -> Unit = {}
    ): ProfileManagementWorkflow {
        return ProfileManagementWorkflow(
            repositoryProvider = { repository },
            currentTimeMillis = currentTimeMillis,
            gameDisplayNameProvider = { gameId ->
                when (gameId) {
                    "skyrim_le" -> "Skyrim Legendary Edition"
                    "fallout_nv" -> "Fallout New Vegas"
                    else -> gameId
                }
            },
            firstSetupInputProvider = firstSetupInputProvider,
            additionalProfileInputProvider = additionalProfileInputProvider,
            activeProfileIdProvider = activeProfileIdProvider,
            dashboardProfileInputProvider = dashboardProfileInputProvider,
            applyFirstSetupUiState = applyFirstSetupUiState,
            applyCreatedProfileUiState = applyCreatedProfileUiState,
            applySwitchedProfileUiState = applySwitchedProfileUiState,
            applySavedProfileUiState = applySavedProfileUiState,
            applyDeletedProfileUiStateAsync = applyDeletedProfileUiStateAsync,
            applyDeletedProfileUiStateBlocking = applyDeletedProfileUiStateBlocking,
            saveSelectedGameConfigFromUi = saveSelectedGameConfigFromUi,
            loadSelectedGameConfigIntoUi = loadSelectedGameConfigIntoUi,
            recoverActiveProfile = recoverActiveProfile,
            syncPluginsFromCurrentState = syncPluginsFromCurrentState,
            refreshDashboard = refreshDashboard,
            appendLog = {},
            appendError = {},
            updateLastOperationStatus = updateLastOperationStatus
        )
    }

    private fun profile(id: String, name: String, gameId: String): GameProfile {
        return GameProfile(
            profileId = id,
            profileName = name,
            gameId = gameId,
            gameDisplayName = gameId,
            targetDataPath = "",
            targetRootPath = "",
            realDeployEnabled = false,
            iniPresetId = null
        )
    }
}
