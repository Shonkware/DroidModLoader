package com.shonkware.droidmodloader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shonkware.droidmodloader.ui.theme.DmlColors
import com.shonkware.droidmodloader.ui.theme.DmlDefaults

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