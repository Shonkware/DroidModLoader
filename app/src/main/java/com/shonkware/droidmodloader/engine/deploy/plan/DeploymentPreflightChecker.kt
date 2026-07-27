package com.shonkware.droidmodloader.engine.deploy.plan

import com.shonkware.droidmodloader.engine.deploy.GameTargetValidationFinding
import com.shonkware.droidmodloader.engine.deploy.GameTargetValidationResult
import com.shonkware.droidmodloader.engine.deploy.GameTargetValidationSeverity
import com.shonkware.droidmodloader.engine.deploy.GameTargetValidator
import com.shonkware.droidmodloader.engine.deploy.GameTargetType
import com.shonkware.droidmodloader.engine.model.GameDeploymentConfig
import java.io.File

class DeploymentPreflightChecker(
    private val gameTargetValidator: GameTargetValidator = GameTargetValidator()
) {

    fun check(
        config: GameDeploymentConfig?,
        plan: ScopedDeploymentPlan
    ): DeploymentPreflightResult {
        val issues = mutableListOf<DeploymentPreflightIssue>()

        checkConfig(config, issues)
        checkTargets(config, plan, issues)
        checkPlanPaths(plan, issues)
        checkSourceFiles(plan, issues)

        if (issues.isEmpty()) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.INFO,
                    title = "No preflight issues found.",
                    details = "The current deploy plan did not find obvious safety problems."
                )
            )
        }

        return DeploymentPreflightResult(issues)
    }

    private fun checkConfig(
        config: GameDeploymentConfig?,
        issues: MutableList<DeploymentPreflightIssue>
    ) {
        if (config == null) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.WARNING,
                    title = "No saved game deploy config found.",
                    details = "The app may use fallback test output folders instead of a selected real target."
                )
            )
            return
        }

        if (!config.realDeployEnabled) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.INFO,
                    title = "Real deploy is disabled.",
                    details = "Deploy will use the app's test output folders instead of real game folders."
                )
            )
        }
    }

    private fun checkTargets(
        config: GameDeploymentConfig?,
        plan: ScopedDeploymentPlan,
        issues: MutableList<DeploymentPreflightIssue>
    ) {
        if (config == null || !config.realDeployEnabled) return

        val dataResult = if (config.dataPathReselectionRequired) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.ERROR,
                    title = "Data target must be reselected.",
                    details = "The previous folder permission cannot be converted into a direct path."
                )
            )
            null
        } else {
            validateTarget(
                gameId = config.gameId,
                targetType = GameTargetType.DATA,
                path = config.targetDataPath,
                required = true,
                issues = issues
            )
        }

        val rootOperationsNeeded = plan.rootPlan.operationCount > 0
        val rootResult = if (config.rootPathReselectionRequired && rootOperationsNeeded) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.ERROR,
                    title = "Game Root target must be reselected.",
                    details = "The previous folder permission cannot be converted into a direct path."
                )
            )
            null
        } else if (config.targetRootPath.isNotBlank() || rootOperationsNeeded) {
            validateTarget(
                gameId = config.gameId,
                targetType = GameTargetType.GAME_ROOT,
                path = config.targetRootPath,
                required = rootOperationsNeeded,
                issues = issues
            )
        } else {
            null
        }

        if (dataResult != null && rootResult != null) {
            gameTargetValidator.validateRelationship(
                dataResult = dataResult,
                rootResult = rootResult
            ).forEach { finding -> issues.add(finding.toPreflightIssue()) }
        }
    }

    private fun validateTarget(
        gameId: String,
        targetType: GameTargetType,
        path: String,
        required: Boolean,
        issues: MutableList<DeploymentPreflightIssue>
    ): GameTargetValidationResult? {
        if (path.isBlank()) {
            if (required) {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "${targetType.displayName} target is not selected.",
                        details = "Choose a direct filesystem folder before real deploy."
                    )
                )
            }
            return null
        }

        return gameTargetValidator.validateTarget(
            gameId = gameId,
            targetType = targetType,
            path = path
        ).also { result ->
            result.findings.forEach { finding ->
                issues.add(finding.toPreflightIssue())
            }
        }
    }

    private fun GameTargetValidationFinding.toPreflightIssue(): DeploymentPreflightIssue {
        return DeploymentPreflightIssue(
            severity = when (severity) {
                GameTargetValidationSeverity.INFO -> DeploymentPreflightSeverity.INFO
                GameTargetValidationSeverity.WARNING -> DeploymentPreflightSeverity.WARNING
                GameTargetValidationSeverity.ERROR -> DeploymentPreflightSeverity.ERROR
            },
            title = title,
            details = details
        )
    }

    private fun checkPlanPaths(
        plan: ScopedDeploymentPlan,
        issues: MutableList<DeploymentPreflightIssue>
    ) {
        val allOperations = plan.dataPlan.operations + plan.rootPlan.operations

        for (operation in allOperations) {
            val path = operation.normalizedPath

            if (path.isBlank()) {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "Blank deploy path found.",
                        details = operation.type.name
                    )
                )
                continue
            }

            if (path.startsWith("/") ||
                path.startsWith("\\") ||
                path.contains("\\") ||
                path.split("/").any { it == ".." } ||
                path.contains(":")
            ) {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "Unsafe deploy path found.",
                        details = path
                    )
                )
            }
        }
    }

    private fun checkSourceFiles(
        plan: ScopedDeploymentPlan,
        issues: MutableList<DeploymentPreflightIssue>
    ) {
        val operations = plan.dataPlan.operations + plan.rootPlan.operations

        val sourceOperations = operations.filter {
            it.type == DeploymentPlanOperationType.ADD ||
                    it.type == DeploymentPlanOperationType.UPDATE ||
                    it.type == DeploymentPlanOperationType.FORCE_REWRITE
        }
        for (operation in sourceOperations) {
            val sourcePath = operation.newRecord?.sourceFilePath

            if (sourcePath.isNullOrBlank()) {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "Missing source file path.",
                        details = operation.normalizedPath
                    )
                )
                continue
            }

            val sourceFile = File(sourcePath)

            when {
                !sourceFile.exists() -> {
                    issues.add(
                        DeploymentPreflightIssue(
                            severity = DeploymentPreflightSeverity.ERROR,
                            title = "Source file does not exist.",
                            details = "${operation.normalizedPath} -> $sourcePath"
                        )
                    )
                }

                !sourceFile.isFile -> {
                    issues.add(
                        DeploymentPreflightIssue(
                            severity = DeploymentPreflightSeverity.ERROR,
                            title = "Source path is not a file.",
                            details = "${operation.normalizedPath} -> $sourcePath"
                        )
                    )
                }

                !sourceFile.canRead() -> {
                    issues.add(
                        DeploymentPreflightIssue(
                            severity = DeploymentPreflightSeverity.ERROR,
                            title = "Source file is not readable.",
                            details = "${operation.normalizedPath} -> $sourcePath"
                        )
                    )
                }
            }
        }
    }
}