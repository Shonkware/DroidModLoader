package com.shonkware.droidmodloader.ui.workflow

internal class DeployRecoveryWorkflowController(
    private val runInBackground: (() -> Unit) -> Unit,
    private val showRecoveryDetails: () -> Unit,
    private val hideRecoveryDetails: () -> Unit,
    private val dismissRecoveryWarning: () -> Unit,
    private val viewLastDeployJournal: () -> Unit,
    private val markDeployRecoveryReviewed: () -> Unit
) {

    fun openRecoveryDetails() {
        showRecoveryDetails()
    }

    fun closeRecoveryDetails() {
        hideRecoveryDetails()
    }

    fun dismissWarning() {
        dismissRecoveryWarning()
    }

    fun viewLastJournal() {
        runInBackground {
            viewLastDeployJournal()
        }
    }

    fun markReviewed() {
        runInBackground {
            markDeployRecoveryReviewed()
        }
    }
}