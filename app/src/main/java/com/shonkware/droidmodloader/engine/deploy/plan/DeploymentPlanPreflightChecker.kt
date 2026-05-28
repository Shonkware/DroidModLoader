package com.shonkware.droidmodloader.engine.deploy.plan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.shonkware.droidmodloader.engine.model.GameDeploymentConfig
import java.io.File

class DeploymentPreflightChecker(
    private val context: Context
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
        if (config == null) return

        if (!config.realDeployEnabled) {
            return
        }

        val hasDataTarget =
            !config.targetTreeUri.isNullOrBlank() ||
                    config.targetDataPath.isNotBlank()

        if (!hasDataTarget) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.ERROR,
                    title = "Data target is not selected.",
                    details = "Pick the game's Data folder before real deploy."
                )
            )
        } else {
            checkTreeOrPathTarget(
                label = "Data target",
                treeUri = config.targetTreeUri,
                path = config.targetDataPath,
                issues = issues
            )
        }

        val rootOperationsNeeded = plan.rootPlan.operationCount > 0

        val hasRootTarget =
            !config.targetRootTreeUri.isNullOrBlank() ||
                    config.targetRootPath.isNotBlank()

        if (rootOperationsNeeded && !hasRootTarget) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.WARNING,
                    title = "Game Root operations exist, but no Game Root target is selected.",
                    details = "Root files may only deploy to the app's test output folder. Pick Game Root for SKSE, NVSE, ENB, DLL loaders, or root EXE files."
                )
            )
        }

        if (hasRootTarget) {
            checkTreeOrPathTarget(
                label = "Game Root target",
                treeUri = config.targetRootTreeUri,
                path = config.targetRootPath,
                issues = issues
            )
        }
    }

    private fun checkTreeOrPathTarget(
        label: String,
        treeUri: String?,
        path: String,
        issues: MutableList<DeploymentPreflightIssue>
    ) {
        if (!treeUri.isNullOrBlank()) {
            try {
                val documentFile = DocumentFile.fromTreeUri(
                    context,
                    Uri.parse(treeUri)
                )

                when {
                    documentFile == null -> {
                        issues.add(
                            DeploymentPreflightIssue(
                                severity = DeploymentPreflightSeverity.ERROR,
                                title = "$label could not be opened.",
                                details = "Android did not return a valid folder for the saved Tree URI."
                            )
                        )
                    }

                    !documentFile.exists() -> {
                        issues.add(
                            DeploymentPreflightIssue(
                                severity = DeploymentPreflightSeverity.ERROR,
                                title = "$label does not exist.",
                                details = treeUri
                            )
                        )
                    }

                    !documentFile.isDirectory -> {
                        issues.add(
                            DeploymentPreflightIssue(
                                severity = DeploymentPreflightSeverity.ERROR,
                                title = "$label is not a folder.",
                                details = treeUri
                            )
                        )
                    }

                    else -> {
                        issues.add(
                            DeploymentPreflightIssue(
                                severity = DeploymentPreflightSeverity.INFO,
                                title = "$label is available.",
                                details = "Tree URI target is readable."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "$label check failed.",
                        details = e.message ?: treeUri
                    )
                )
            }

            return
        }

        if (path.isBlank()) {
            issues.add(
                DeploymentPreflightIssue(
                    severity = DeploymentPreflightSeverity.ERROR,
                    title = "$label path is blank."
                )
            )
            return
        }

        val folder = File(path)

        when {
            !folder.exists() -> {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "$label path does not exist.",
                        details = path
                    )
                )
            }

            !folder.isDirectory -> {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "$label path is not a folder.",
                        details = path
                    )
                )
            }

            !folder.canRead() -> {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.ERROR,
                        title = "$label path is not readable.",
                        details = path
                    )
                )
            }

            else -> {
                issues.add(
                    DeploymentPreflightIssue(
                        severity = DeploymentPreflightSeverity.INFO,
                        title = "$label is available.",
                        details = path
                    )
                )
            }
        }
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
                    it.type == DeploymentPlanOperationType.UPDATE
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