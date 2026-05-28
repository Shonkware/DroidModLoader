package com.shonkware.droidmodloader.engine.deploy.journal

enum class DeploymentJournalStatus {
    STARTED,
    COMPLETED,
    FAILED
}

data class DeploymentJournalRecord(
    val schemaVersion: Int = 1,
    val operationId: String,
    val gameId: String,
    val profileId: String,
    val status: DeploymentJournalStatus,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val planSummary: DeploymentJournalPlanSummary,
    val resultSummary: DeploymentJournalResultSummary?,
    val failureMessage: String?
) {
    fun toDebugSummary(): String {
        return buildString {
            appendLine("Deploy Journal")
            appendLine("  Operation ID: $operationId")
            appendLine("  Game: $gameId")
            appendLine("  Profile: $profileId")
            appendLine("  Status: $status")
            appendLine("  Started: $startedAtEpochMillis")
            appendLine("  Completed: ${completedAtEpochMillis ?: "not completed"}")

            appendLine()
            appendLine("Plan:")
            appendLine("  Data operations: ${planSummary.dataOperationCount}")
            appendLine("  Game Root operations: ${planSummary.rootOperationCount}")
            appendLine("  Total operations: ${planSummary.totalOperationCount}")
            appendLine("  Data copy bytes: ${planSummary.dataEstimatedCopyBytes ?: "unknown"}")
            appendLine("  Game Root copy bytes: ${planSummary.rootEstimatedCopyBytes ?: "unknown"}")
            appendLine("  Preflight can deploy: ${planSummary.preflightCanDeploy}")
            appendLine("  Preflight errors: ${planSummary.preflightErrorCount}")
            appendLine("  Preflight warnings: ${planSummary.preflightWarningCount}")

            if (resultSummary != null) {
                appendLine()
                appendLine("Result:")
                appendLine("  Adds: ${resultSummary.addCount}")
                appendLine("  Updates: ${resultSummary.updateCount}")
                appendLine("  Removes: ${resultSummary.removeCount}")
                appendLine("  Backups created: ${resultSummary.backupCount}")
                appendLine("  Backups restored: ${resultSummary.restoreCount}")
                appendLine("  Protected conflicts: ${resultSummary.protectedConflictCount}")
                appendLine("  Final file count: ${resultSummary.finalRecordCount}")
            }

            if (!failureMessage.isNullOrBlank()) {
                appendLine()
                appendLine("Failure:")
                appendLine("  $failureMessage")
            }
        }
    }
}

data class DeploymentJournalPlanSummary(
    val dataOperationCount: Int,
    val rootOperationCount: Int,
    val totalOperationCount: Int,
    val dataEstimatedCopyBytes: Long?,
    val rootEstimatedCopyBytes: Long?,
    val preflightCanDeploy: Boolean,
    val preflightErrorCount: Int,
    val preflightWarningCount: Int
)

data class DeploymentJournalResultSummary(
    val addCount: Int,
    val updateCount: Int,
    val removeCount: Int,
    val backupCount: Int,
    val restoreCount: Int,
    val protectedConflictCount: Int,
    val finalRecordCount: Int
)