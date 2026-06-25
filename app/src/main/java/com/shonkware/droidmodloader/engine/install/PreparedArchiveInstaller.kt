package com.shonkware.droidmodloader.engine.install

import android.util.Log
import java.io.File
import java.io.IOException

class PreparedArchiveInstaller internal constructor(
    private val tempDir: File,
    private val modsDir: File,
    private val directoryReplacer: InstalledModDirectoryReplacer,
    private val operationId: () -> String,
    private val debugLog: (String) -> Unit
) {
    constructor(
        tempDir: File,
        modsDir: File
    ) : this(
        tempDir = tempDir,
        modsDir = modsDir,
        directoryReplacer = InstalledModDirectoryReplacer(),
        operationId = { System.currentTimeMillis().toString() },
        debugLog = { message ->
            Log.d(TAG, message)
        }
    )

    companion object {
        private const val TAG = "DroidModLoader"
    }

    private val layoutAnalyzer = InstallerLayoutAnalyzer()
    private val archiveExtractor = ArchiveExtractor()

    fun prepare(archive: File): PreparedArchiveInstall {
        val sessionRoot = File(
            tempDir,
            "installer_sessions/${operationId()}"
        )
        val rawRoot = File(sessionRoot, "raw")

        try {
            if (sessionRoot.exists()) {
                throw IOException(
                    "Prepared installer session already exists: " +
                            sessionRoot.absolutePath
                )
            }

            if (!rawRoot.mkdirs()) {
                throw IOException(
                    "Could not create installer raw folder: " +
                            rawRoot.absolutePath
                )
            }

            archiveExtractor.extractToRawFolder(
                archive = archive,
                outputDir = rawRoot
            )

            val contentRoot = layoutAnalyzer.resolveContentRoot(rawRoot)
            val modName = archive.nameWithoutExtension
            val plan = layoutAnalyzer.analyze(
                contentRoot = contentRoot,
                modName = modName
            )

            debugLog(
                "Prepared installer plan: " +
                        "type=${plan.installerType}, " +
                        "groups=${plan.groups.size}"
            )

            return PreparedArchiveInstall(
                archivePath = archive.absolutePath,
                archiveName = archive.name,
                modName = modName,
                sessionRootPath = sessionRoot.absolutePath,
                extractedRootPath = rawRoot.absolutePath,
                installRootPath = contentRoot.absolutePath,
                plan = plan
            )
        } catch (exception: Exception) {
            if (
                sessionRoot.exists() &&
                !sessionRoot.deleteRecursively()
            ) {
                exception.addSuppressed(
                    IOException(
                        "Could not clean failed prepared installer session: " +
                                sessionRoot.absolutePath
                    )
                )
            }

            throw IOException(
                "Failed to prepare archive install for ${archive.name}: " +
                        exception.message,
                exception
            )
        }
    }

    fun finalizeInstall(
        prepared: PreparedArchiveInstall,
        selection: InstallerSelection
    ): File {
        ensureDirectoryExists(
            directory = modsDir,
            description = "installed-mod directory"
        )

        val finalDir = File(modsDir, prepared.modName)
        val stagedDir = File(
            modsDir,
            "_installing_${prepared.modName}_${operationId()}"
        )
        val installRoot = File(prepared.installRootPath)

        if (!installRoot.exists() || !installRoot.isDirectory) {
            throw IOException(
                "Prepared install root does not exist: " +
                        installRoot.absolutePath
            )
        }

        if (stagedDir.exists()) {
            throw IOException(
                "Staged prepared install already exists: " +
                        stagedDir.absolutePath
            )
        }

        if (!stagedDir.mkdirs()) {
            throw IOException(
                "Could not create staged prepared install folder: " +
                        stagedDir.absolutePath
            )
        }

        try {
            val selectedOptions = prepared.plan.groups
                .flatMap { it.options }
                .filter { option ->
                    option.required ||
                            selection.selectedOptionIds.contains(option.id)
                }

            if (selectedOptions.isEmpty()) {
                throw IllegalStateException(
                    "No installer options selected."
                )
            }

            selectedOptions.forEach { option ->
                copyOption(
                    installRoot = installRoot,
                    option = option,
                    finalDir = stagedDir
                )
            }

            val installedDir = directoryReplacer.replace(
                stagedDir = stagedDir,
                finalDir = finalDir
            )

            cleanCompletedSession(prepared)

            return installedDir
        } catch (exception: Exception) {
            val installFailure = IOException(
                "Failed to finalize archive install for " +
                        "${prepared.archiveName}: ${exception.message}",
                exception
            )

            if (
                stagedDir.exists() &&
                !stagedDir.deleteRecursively()
            ) {
                installFailure.addSuppressed(
                    IOException(
                        "Could not clean failed prepared install staging: " +
                                stagedDir.absolutePath
                    )
                )
            }

            throw installFailure
        }
    }

    fun cancel(prepared: PreparedArchiveInstall) {
        val sessionRoot = File(prepared.sessionRootPath)

        if (
            sessionRoot.exists() &&
            !sessionRoot.deleteRecursively()
        ) {
            throw IOException(
                "Could not remove prepared installer session: " +
                        sessionRoot.absolutePath
            )
        }
    }

    private fun cleanCompletedSession(
        prepared: PreparedArchiveInstall
    ) {
        val sessionRoot = File(prepared.sessionRootPath)

        if (
            sessionRoot.exists() &&
            !sessionRoot.deleteRecursively()
        ) {
            debugLog(
                "Prepared install succeeded, but its session " +
                        "could not be removed: ${sessionRoot.absolutePath}"
            )
        }
    }

    private fun ensureDirectoryExists(
        directory: File,
        description: String
    ) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException(
                "Could not create $description: " +
                        directory.absolutePath
            )
        }

        if (!directory.isDirectory) {
            throw IOException(
                "Configured $description is not a directory: " +
                        directory.absolutePath
            )
        }
    }

    private fun copyOption(
        installRoot: File,
        option: InstallerOption,
        finalDir: File
    ) {
        val source = if (
            isCurrentDirectoryPath(option.sourcePath)
        ) {
            installRoot
        } else {
            safeResolveInstallerPath(
                root = installRoot,
                relativePath = option.sourcePath
            )
        }

        if (!source.exists()) {
            throw IOException(
                "Installer source does not exist: " +
                        source.absolutePath
            )
        }

        if (source.isDirectory) {
            copyDirectoryOption(
                source = source,
                option = option,
                finalDir = finalDir
            )
        } else {
            copyFileOption(
                source = source,
                option = option,
                finalDir = finalDir
            )
        }
    }

    private fun copyDirectoryOption(
        source: File,
        option: InstallerOption,
        finalDir: File
    ) {
        val destinationRoot = if (
            isCurrentDirectoryPath(option.destinationPath)
        ) {
            finalDir
        } else {
            safeResolveInstallerPath(
                root = finalDir,
                relativePath = option.destinationPath
            )
        }

        if (
            !destinationRoot.exists() &&
            !destinationRoot.mkdirs()
        ) {
            throw IOException(
                "Could not create installer destination: " +
                        destinationRoot.absolutePath
            )
        }

        // Copy the selected folder as-is. Do not auto-collapse Data here;
        // mixed root/Data mods such as SKSE must retain root-level files.
        if (!source.copyRecursively(
                target = destinationRoot,
                overwrite = true
            )
        ) {
            throw IOException(
                "Could not copy installer directory: " +
                        source.absolutePath
            )
        }
    }

    private fun copyFileOption(
        source: File,
        option: InstallerOption,
        finalDir: File
    ) {
        val destination = if (
            isCurrentDirectoryPath(option.destinationPath)
        ) {
            File(finalDir, source.name)
        } else {
            val rawDestination = safeResolveInstallerPath(
                root = finalDir,
                relativePath = option.destinationPath
            )

            if (
                option.destinationPath.endsWith("/") ||
                option.destinationPath.endsWith("\\")
            ) {
                File(rawDestination, source.name)
            } else {
                rawDestination
            }
        }

        val parent = destination.parentFile
            ?: throw IOException(
                "Installer destination has no parent: " +
                        destination.absolutePath
            )

        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException(
                "Could not create installer destination parent: " +
                        parent.absolutePath
            )
        }

        source.copyTo(
            target = destination,
            overwrite = true
        )
    }

    private fun isCurrentDirectoryPath(path: String): Boolean {
        val normalized = path
            .replace("\\", "/")
            .trim()
            .trim('/')

        return normalized.isBlank() || normalized == "."
    }

    private fun normalizeInstallerRelativePath(
        relativePath: String
    ): String {
        return when (
            val normalized = ArchiveEntryPath.normalize(relativePath)
        ) {
            ArchiveEntryPathResult.Ignore -> {
                throw IOException(
                    "Installer path is empty or invalid: " +
                            relativePath
                )
            }

            is ArchiveEntryPathResult.Valid -> {
                normalized.relativePath
            }
        }
    }

    private fun safeResolveInstallerPath(
        root: File,
        relativePath: String
    ): File {
        val normalizedPath =
            normalizeInstallerRelativePath(relativePath)

        return ArchiveEntryPath.safeResolve(
            root = root,
            relativePath = normalizedPath
        )
    }
}