package com.shonkware.droidmodloader.ui.workflow

internal class InstallerWorkflowController(
    private val runInBackground: (() -> Unit) -> Unit,
    private val finalizeInstallerInstall: () -> Unit,
    private val cancelInstallerInstall: () -> Unit,
    private val toggleInstallerOption: (String) -> Unit
) {

    fun finalizeInstall() {
        runInBackground {
            finalizeInstallerInstall()
        }
    }

    fun cancelInstall() {
        cancelInstallerInstall()
    }

    fun toggleOption(optionId: String) {
        toggleInstallerOption(optionId)
    }
}