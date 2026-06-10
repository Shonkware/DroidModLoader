package com.shonkware.droidmodloader.ui.workflow

import com.shonkware.droidmodloader.engine.model.Mod

internal class ModActionWorkflowController(
    private val runInBackground: (() -> Unit) -> Unit,
    private val onToggleModEnabled: (String) -> Unit,
    private val onMoveModUp: (String) -> Unit,
    private val onMoveModDown: (String) -> Unit,
    private val onRequestDeleteMod: (Mod) -> Unit,
    private val onViewModFiles: (String) -> Unit,
    private val onApplyModOrder: (List<String>) -> Unit
) {

    fun toggleMod(modId: String) {
        runInBackground {
            onToggleModEnabled(modId)
        }
    }

    fun moveModUp(modId: String) {
        runInBackground {
            onMoveModUp(modId)
        }
    }

    fun moveModDown(modId: String) {
        runInBackground {
            onMoveModDown(modId)
        }
    }

    fun requestDeleteMod(mod: Mod) {
        onRequestDeleteMod(mod)
    }

    fun viewModFiles(modId: String) {
        runInBackground {
            onViewModFiles(modId)
        }
    }

    fun applyModOrder(orderedModIds: List<String>) {
        runInBackground {
            onApplyModOrder(orderedModIds)
        }
    }
}