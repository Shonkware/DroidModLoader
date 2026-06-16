package com.shonkware.droidmodloader.ui.workflow

internal class PreviewDialogActionWorkflowController(
    private val toggleInstallerFullscreen: () -> Unit,
    private val closeModFilePreview: () -> Unit,
    private val toggleModFilePreviewFullscreen: () -> Unit
) {

    fun toggleInstallerFullscreen() {
        toggleInstallerFullscreen.invoke()
    }

    fun closeModFilePreview() {
        closeModFilePreview.invoke()
    }

    fun toggleModFilePreviewFullscreen() {
        toggleModFilePreviewFullscreen.invoke()
    }
}
