package com.shonkware.droidmodloader.ui.workflow

internal class ProfileWorkflowController(
    private val runInBackground: (() -> Unit) -> Unit,
    private val completeFirstSetup: () -> Unit,
    private val createAdditionalProfile: () -> Unit,
    private val switchActiveProfile: (String) -> Unit,
    private val deleteProfileAction: (String) -> Unit,
    private val saveDashboardSettings: () -> Unit
) {

    fun completeSetup() {
        runInBackground {
            completeFirstSetup()
        }
    }

    fun createProfile() {
        runInBackground {
            createAdditionalProfile()
        }
    }

    fun switchProfile(profileId: String) {
        runInBackground {
            switchActiveProfile(profileId)
        }
    }

    fun deleteProfile(profileId: String) {
        runInBackground {
            deleteProfileAction(profileId)
        }
    }

    fun saveSettings() {
        runInBackground {
            saveDashboardSettings()
        }
    }
}