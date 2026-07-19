package com.shonkware.droidmodloader.ui.workflow

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileWorkflowControllerTest {

    @Test
    fun `delete profile routes profile id to delete action exactly once`() {
        var deleteCallCount = 0
        var deletedProfileId: String? = null

        val controller = ProfileWorkflowController(
            runInBackground = { action -> action() },
            completeFirstSetup = {},
            createAdditionalProfile = {},
            switchActiveProfile = {},
            deleteProfileAction = { profileId ->
                deleteCallCount += 1
                deletedProfileId = profileId
            },
            saveDashboardSettings = {}
        )

        controller.deleteProfile("delete-test-profile")

        assertEquals(1, deleteCallCount)
        assertEquals("delete-test-profile", deletedProfileId)
    }
}
