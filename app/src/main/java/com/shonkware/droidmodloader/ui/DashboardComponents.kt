package com.shonkware.droidmodloader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shonkware.droidmodloader.engine.index.ModContentIndex
import com.shonkware.droidmodloader.engine.index.ModFileFolderSummary
import com.shonkware.droidmodloader.engine.index.ModFilePreview
import com.shonkware.droidmodloader.engine.index.ModFilePreviewStatus
import com.shonkware.droidmodloader.engine.model.Mod
import com.shonkware.droidmodloader.engine.model.PluginEntry
import com.shonkware.droidmodloader.engine.overwrite.OverwriteEntry
import com.shonkware.droidmodloader.ui.theme.DmlDefaults
import com.shonkware.droidmodloader.ui.theme.DmlColors

@Composable
fun DeveloperToolsCard(
    operationInProgress: Boolean,
    onBuildResolvedDataGraph: () -> Unit,
    onBuildDeploymentPlan: () -> Unit,
    onShowArchiveLibrarySummary: () -> Unit,
    onRepairV050Artifacts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = DmlDefaults.panelCardColors(),
        border = BorderStroke(1.dp, DmlColors.BorderDim)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Developer Tools", fontWeight = FontWeight.Bold)

            Text(
                text = "Advanced tools for testing and repair. Most users should not need these every day.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                enabled = !operationInProgress,
                onClick = onBuildResolvedDataGraph,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Build Resolved Graph Summary")
            }

            Text(
                text = "Developer check: builds the current profile's resolved game view summary without deploying or modifying target files.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                enabled = !operationInProgress,
                onClick = onBuildDeploymentPlan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Build Deploy Plan Summary")
            }

            Text(
                text = "Developer check: compares the current resolved winners against the saved deploy manifests. No files are changed.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                enabled = !operationInProgress,
                onClick = onShowArchiveLibrarySummary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Archive Library Summary")
            }

            Text(
                text = "Developer check: lists saved imported archive metadata for the active profile without changing files.",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Advanced repair tools. Use these only when troubleshooting beta builds.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                enabled = !operationInProgress,
                onClick = onRepairV050Artifacts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Repair v0.5.0-beta Artifacts")
            }

            Text(
                text = "Repairs earlier beta artifacts such as .ini.txt/.xml.txt files, duplicate folders like sound(1), and incorrectly wrapped installed mod folders. Existing correct files are not overwritten.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ModsPanelDialog(
    mods: List<Mod>,
    modContentIndexes: Map<String, ModContentIndex>,
    onToggleMod: (String) -> Unit,
    onMoveModUp: (String) -> Unit,
    onMoveModDown: (String) -> Unit,
    onDeleteMod: (Mod) -> Unit,
    onViewModFiles: (String) -> Unit,
    onApplyModOrder: (List<String>) -> Unit,
    onOpenOverwriteFolder: () -> Unit,
    onClose: () -> Unit
) {
    var orderedMods by remember(mods) {
        mutableStateOf(mods.sortedBy { it.priority })
    }

    fun moveMod(modId: String, direction: Int) {
        val index = orderedMods.indexOfFirst { it.id == modId }
        if (index == -1) return

        val target = (index + direction).coerceIn(0, orderedMods.lastIndex)
        if (target == index) return

        val mutable = orderedMods.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        orderedMods = mutable
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mods",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenOverwriteFolder) {
                            Text("Overwrite")
                        }

                        Button(onClick = {
                            onApplyModOrder(orderedMods.map { it.id })
                        }) {
                            Text("Save Order")
                        }

                        Button(onClick = onClose) {
                            Text("Close")
                        }
                    }
                }

                Text(
                    text = "Long-press and drag ☰ to reorder. Tap Save Order when done.",
                    style = MaterialTheme.typography.bodySmall
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = orderedMods,
                        key = { _, mod -> mod.id }
                    ) { _, mod ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DragHandle(
                                onStep = { direction ->
                                    moveMod(mod.id, direction)
                                },
                                onDragFinished = {
                                    onApplyModOrder(orderedMods.map { it.id })
                                }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                CompactModRow(
                                    mod = mod,
                                    contentIndex = modContentIndexes[mod.id],
                                    onToggleMod = onToggleMod,
                                    onMoveModUp = onMoveModUp,
                                    onMoveModDown = onMoveModDown,
                                    onDeleteMod = onDeleteMod,
                                    onViewModFiles = onViewModFiles
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PluginsPanelDialog(
    plugins: List<PluginEntry>,
    onTogglePlugin: (String) -> Unit,
    onMovePluginUp: (String) -> Unit,
    onMovePluginDown: (String) -> Unit,
    onApplyPluginOrder: (List<String>) -> Unit,
    onClose: () -> Unit
) {
    var orderedPlugins by remember(plugins) {
        mutableStateOf(plugins.sortedBy { it.priority })
    }

    fun movePlugin(pluginPath: String, direction: Int) {
        val index = orderedPlugins.indexOfFirst { it.normalizedPath == pluginPath }
        if (index == -1) return

        val plugin = orderedPlugins[index]
        if (plugin.locked) return

        val target = (index + direction).coerceIn(0, orderedPlugins.lastIndex)
        if (target == index) return
        if (orderedPlugins[target].locked) return

        val mutable = orderedPlugins.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        orderedPlugins = mutable
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plugins",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            onApplyPluginOrder(orderedPlugins.map { it.normalizedPath })
                        }) {
                            Text("Save Order")
                        }

                        Button(onClick = onClose) {
                            Text("Close")
                        }
                    }
                }

                Text(
                    text = "Long-press and drag ☰ to reorder unlocked plugins. Official plugins stay pinned.",
                    style = MaterialTheme.typography.bodySmall
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = orderedPlugins,
                        key = { _, plugin -> plugin.normalizedPath }
                    ) { _, plugin ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DragHandle(
                                enabled = !plugin.locked,
                                onStep = { direction ->
                                    movePlugin(plugin.normalizedPath, direction)
                                },
                                onDragFinished = {
                                    onApplyPluginOrder(orderedPlugins.map { it.normalizedPath })
                                }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                PluginRow(
                                    plugin = plugin,
                                    onTogglePlugin = onTogglePlugin,
                                    onMovePluginUp = onMovePluginUp,
                                    onMovePluginDown = onMovePluginDown
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DragHandle(
    enabled: Boolean = true,
    onStep: (Int) -> Unit,
    onDragFinished: () -> Unit
) {
    val thresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var accumulatedY by remember { mutableStateOf(0f) }

    Text(
        text = "☰",
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(8.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                accumulatedY = 0f
                            },
                            onDragEnd = {
                                accumulatedY = 0f
                                onDragFinished()
                            },
                            onDragCancel = {
                                accumulatedY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedY += dragAmount.y

                                while (accumulatedY > thresholdPx) {
                                    onStep(1)
                                    accumulatedY -= thresholdPx
                                }

                                while (accumulatedY < -thresholdPx) {
                                    onStep(-1)
                                    accumulatedY += thresholdPx
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    )
}
