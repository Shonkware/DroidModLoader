package com.shonkware.droidmodloader.engine.deploy.plan

data class DeploymentPreflightResult(
    val issues: List<DeploymentPreflightIssue>
) {
    val errorCount: Int
        get() = issues.count { it.severity == DeploymentPreflightSeverity.ERROR }

    val warningCount: Int
        get() = issues.count { it.severity == DeploymentPreflightSeverity.WARNING }

    val infoCount: Int
        get() = issues.count { it.severity == DeploymentPreflightSeverity.INFO }

    val canDeploy: Boolean
        get() = errorCount == 0

    fun toDebugSummary(): String {
        return buildString {
            appendLine("Deploy Readiness Check")
            appendLine("  Can deploy: ${if (canDeploy) "yes" else "no"}")
            appendLine("  Errors: $errorCount")
            appendLine("  Warnings: $warningCount")
            appendLine("  Info: $infoCount")

            if (issues.isNotEmpty()) {
                appendLine()
                appendLine("Findings:")

                for (issue in issues) {
                    appendLine("  ${issue.severity}: ${issue.title}")

                    if (issue.details.isNotBlank()) {
                        appendLine("    ${issue.details}")
                    }
                }
            }
        }
    }
}

data class DeploymentPreflightIssue(
    val severity: DeploymentPreflightSeverity,
    val title: String,
    val details: String = ""
)

enum class DeploymentPreflightSeverity {
    INFO,
    WARNING,
    ERROR
}