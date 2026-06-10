package com.shonkware.droidmodloader.ui.workflow

internal class FolderPickerWorkflowController(
    private val runInBackground: (() -> Unit) -> Unit,
    private val savePickedDataFolderToSelectedGameConfig: (String) -> Unit,
    private val savePickedRootFolderToSelectedGameConfig: (String) -> Unit,
    private val setNewProfileTreeUriText: (String) -> Unit,
    private val appendLog: (String) -> Unit
) {

    fun handlePickedFolder(
        mode: FolderPickMode,
        treeUri: String
    ) {
        runInBackground {
            when (mode) {
                FolderPickMode.ActiveDataFolder -> {
                    savePickedDataFolderToSelectedGameConfig(treeUri)
                }

                FolderPickMode.ActiveGameRootFolder -> {
                    savePickedRootFolderToSelectedGameConfig(treeUri)
                }

                FolderPickMode.NewProfileDataFolder -> {
                    setNewProfileTreeUriText(treeUri)
                    appendLog("Selected Data folder for new profile.")
                }
            }
        }
    }
}