package com.shonkware.droidmodloader.engine.deploy.plan

import com.shonkware.droidmodloader.engine.model.DeploymentRecord
import com.shonkware.droidmodloader.engine.model.FileRecord

enum class DeploymentPlanScope {
    DATA,
    GAME_ROOT
}

enum class DeploymentPlanOperationType {
    ADD,
    UPDATE,
    FORCE_REWRITE,
    REMOVE,
    RESTORE_BACKUP
}

data class DeploymentPlan(
    val scope: DeploymentPlanScope,
    val operations: List<DeploymentPlanOperation>
) {
    val addCount: Int
        get() = operations.count { it.type == DeploymentPlanOperationType.ADD }

    val updateCount: Int
        get() = operations.count { it.type == DeploymentPlanOperationType.UPDATE }

    val removeCount: Int
        get() = operations.count { it.type == DeploymentPlanOperationType.REMOVE }

    val restoreBackupCount: Int
        get() = operations.count { it.type == DeploymentPlanOperationType.RESTORE_BACKUP }

    val operationCount: Int
        get() = operations.size

    val forceRewriteCount: Int
        get() = operations.count { it.type == DeploymentPlanOperationType.FORCE_REWRITE }

    val estimatedBytesToCopy: Long?
        get() {
            val values = operations
                .filter {
                    it.type == DeploymentPlanOperationType.ADD ||
                            it.type == DeploymentPlanOperationType.UPDATE ||
                            it.type == DeploymentPlanOperationType.FORCE_REWRITE
                }
                .map { it.sourceSizeBytes }

            if (values.any { it == null }) return null

            return values.filterNotNull().sum()
        }

    fun toDebugSummary(maxOperations: Int = 20): String {
        return buildString {
            appendLine("${scope.displayName()} Deploy Plan")
            appendLine("  Adds: $addCount")
            appendLine("  Updates: $updateCount")
            appendLine("  Force rewrites: $forceRewriteCount")
            appendLine("  Removes: $removeCount")
            appendLine("  Backup restores: $restoreBackupCount")
            appendLine("  Total operations: $operationCount")

            val bytes = estimatedBytesToCopy
            if (bytes != null) {
                appendLine("  Estimated copy size: ${formatBytes(bytes)}")
            } else {
                appendLine("  Estimated copy size: unknown")
            }

            val sample = operations
                .sortedBy { it.normalizedPath }
                .take(maxOperations)

            if (sample.isNotEmpty()) {
                appendLine()
                appendLine("Sample operations:")

                for (operation in sample) {
                    appendLine("  ${operation.type}: ${operation.normalizedPath}")
                    appendLine("    Reason: ${operation.reason}")

                    if (!operation.winningModName.isNullOrBlank()) {
                        appendLine("    Winner: ${operation.winningModName}")
                    }
                }
            }
        }
    }
}

data class DeploymentPlanOperation(
    val type: DeploymentPlanOperationType,
    val normalizedPath: String,
    val newRecord: FileRecord?,
    val oldRecord: DeploymentRecord?,
    val winningModName: String?,
    val sourceSizeBytes: Long?,
    val reason: String
)

private fun DeploymentPlanScope.displayName(): String {
    return when (this) {
        DeploymentPlanScope.DATA -> "Data"
        DeploymentPlanScope.GAME_ROOT -> "Game Root"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"

    val kib = bytes / 1024.0
    if (kib < 1024.0) return "%.1f KiB".format(kib)

    val mib = kib / 1024.0
    if (mib < 1024.0) return "%.1f MiB".format(mib)

    val gib = mib / 1024.0
    return "%.2f GiB".format(gib)
}