package com.shonkware.droidmodloader.engine.install

import android.util.Log
import java.io.File
import java.io.IOException

class PreparedArchiveInstaller(
    private val tempDir: File,
    private val modsDir: File
) {
    companion object {
        private const val TAG = "DroidModLoader"
    }

    private val layoutAnalyzer = InstallerLayoutAnalyzer()
    private val archiveExtractor = ArchiveExtractor()

    fun prepare(archive: File): PreparedArchiveInstall {
        val sessionRoot = File(tempDir, "installer_sessions/${System.currentTimeMillis()}")
        val rawRoot = File(sessionRoot, "raw")

        try {
            if (sessionRoot.exists()) {
                sessionRoot.deleteRecursively()
            }

            if (!rawRoot.mkdirs()) {
                throw IOException("Could not create installer raw folder: ${rawRoot.absolutePath}")
            }

            archiveExtractor.extractToRawFolder(archive, rawRoot)

            val contentRoot = layoutAnalyzer.resolveContentRoot(rawRoot)
            val modName = archive.nameWithoutExtension
            val plan = layoutAnalyzer.analyze(contentRoot, modName)

            Log.d(
                TAG,
                "Prepared installer plan: type=${plan.installerType}, groups=${plan.groups.size}"
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
        } catch (t: Throwable) {
            if (sessionRoot.exists()) {
                sessionRoot.deleteRecursively()
            }
            throw IOException(
                "Failed to prepare archive install for ${archive.name}: ${t.message}",
                t
            )
        }
    }

    fun finalizeInstall(
        prepared: PreparedArchiveInstall,
        selection: InstallerSelection
    ): File {
        val finalDir = File(modsDir, prepared.modName)
        val tempFinalDir =
            File(modsDir, "_installing_${prepared.modName}_${System.currentTimeMillis()}")
        val installRoot = File(prepared.installRootPath)

        if (!installRoot.exists() || !installRoot.isDirectory) {
            throw IOException("Prepared install root does not exist: ${installRoot.absolutePath}")
        }

        if (tempFinalDir.exists()) {
            tempFinalDir.deleteRecursively()
        }

        if (!tempFinalDir.mkdirs()) {
            throw IOException("Could not create temporary final install folder: ${tempFinalDir.absolutePath}")
        }

        try {
            val selectedOptions = prepared.plan.groups
                .flatMap { it.options }
                .filter { option ->
                    option.required || selection.selectedOptionIds.contains(option.id)
                }

            if (selectedOptions.isEmpty()) {
                throw IllegalStateException("No installer options selected.")
            }

            for (option in selectedOptions) {
                copyOption(
                    installRoot = installRoot,
                    option = option,
                    finalDir = tempFinalDir
                )
            }

            if (finalDir.exists()) {
                finalDir.deleteRecursively()
            }

            if (!tempFinalDir.renameTo(finalDir)) {
                tempFinalDir.copyRecursively(finalDir, overwrite = true)
                tempFinalDir.deleteRecursively()
            }

            File(prepared.sessionRootPath).deleteRecursively()

            return finalDir
        } catch (t: Throwable) {
            if (tempFinalDir.exists()) {
                tempFinalDir.deleteRecursively()
            }
            throw IOException(
                "Failed to finalize archive install for ${prepared.archiveName}: ${t.message}",
                t
            )
        }
    }

    fun cancel(prepared: PreparedArchiveInstall) {
        File(prepared.sessionRootPath).deleteRecursively()
    }

    private fun copyOption(
        installRoot: File,
        option: InstallerOption,
        finalDir: File
    ) {
        val source = if (option.sourcePath == ".") {
            installRoot
        } else {
            safeResolve(installRoot, option.sourcePath)
        }

        if (!source.exists()) {
            throw IOException("Installer source does not exist: ${source.absolutePath}")
        }

        if (source.isDirectory) {
            copyDirectoryOption(source, option, finalDir)
        } else {
            copyFileOption(source, option, finalDir)
        }
    }

    private fun copyDirectoryOption(
        source: File,
        option: InstallerOption,
        finalDir: File
    ) {
        val dataFolder = File(source, "Data")
        val actualSource = if (dataFolder.exists() && dataFolder.isDirectory) {
            dataFolder
        } else {
            source
        }

        val destinationRoot = if (option.destinationPath.isBlank()) {
            finalDir
        } else {
            safeResolve(finalDir, option.destinationPath)
        }

        destinationRoot.mkdirs()
        actualSource.copyRecursively(destinationRoot, overwrite = true)
    }

    private fun copyFileOption(
        source: File,
        option: InstallerOption,
        finalDir: File
    ) {
        val destination = if (option.destinationPath.isBlank()) {
            File(finalDir, source.name)
        } else {
            val rawDestination = safeResolve(finalDir, option.destinationPath)

            if (option.destinationPath.endsWith("/") || option.destinationPath.endsWith("\\")) {
                File(rawDestination, source.name)
            } else {
                rawDestination
            }
        }

        destination.parentFile?.mkdirs()
        source.copyTo(destination, overwrite = true)
    }

    private fun safeResolve(root: File, relativePath: String): File {
        return ArchiveEntryPath.safeResolve(root, relativePath)
    }
}