package com.shonkware.droidmodloader.engine.install

import android.util.Log
import java.io.File
import java.io.IOException

class ModInstaller internal constructor(
    private val tempDir: File,
    private val modsDir: File,
    private val extractArchive:
        (
        File,
        File,
        InstallCancellationSignal
    ) -> Unit,
    private val directoryReplacer:
    InstalledModDirectoryReplacer,
    private val installId: () -> String,
    private val debugLog: (String) -> Unit,
    private val fileCopier:
        (
        File,
        File,
        InstallCancellationSignal
    ) -> Unit =
        InstallFileCopier::copyRecursively
) {
    constructor(
        tempDir: File,
        modsDir: File,
        archiveExtractor:
        ArchiveExtractor = ArchiveExtractor()
    ) : this(
        tempDir = tempDir,
        modsDir = modsDir,
        extractArchive =
            archiveExtractor::extractToRawFolder,
        directoryReplacer =
            InstalledModDirectoryReplacer(),
        installId = {
            System.currentTimeMillis().toString()
        },
        debugLog = { message ->
            Log.d(TAG, message)
        }
    )

    companion object {
        private const val TAG = "DroidModLoader"
    }

    fun installArchive(
        archive: File,
        cancellationSignal:
        InstallCancellationSignal =
            InstallCancellationSignal.NONE
    ): File {
        cancellationSignal
            .throwIfCancellationRequested()

        debugLog(
            "ModInstaller.installArchive start: " +
                    archive.absolutePath
        )
        debugLog(
            "Archive exists=${archive.exists()} " +
                    "size=${archive.length()} " +
                    "extension=${archive.extension}"
        )

        val operationId = installId()
        val extractFolder = File(
            tempDir,
            "direct_install_$operationId"
        )
        val rawRoot =
            File(extractFolder, "raw")
        val modName =
            archive.nameWithoutExtension
        val finalDir =
            File(modsDir, modName)
        val stagedDir = File(
            modsDir,
            "_installing_${modName}_$operationId"
        )

        try {
            ensureDirectoryExists(
                directory = tempDir,
                description =
                    "temporary installer directory"
            )
            ensureDirectoryExists(
                directory = modsDir,
                description =
                    "installed-mod directory"
            )

            if (extractFolder.exists()) {
                throw IOException(
                    "Direct-install session already exists: " +
                            extractFolder.absolutePath
                )
            }

            if (!rawRoot.mkdirs()) {
                throw IOException(
                    "Could not create raw extract folder: " +
                            rawRoot.absolutePath
                )
            }

            if (stagedDir.exists()) {
                throw IOException(
                    "Staged mod directory already exists: " +
                            stagedDir.absolutePath
                )
            }

            extractArchive(
                archive,
                rawRoot,
                cancellationSignal
            )

            cancellationSignal
                .throwIfCancellationRequested()

            val normalizedRoot =
                normalizeExtractedStructure(rawRoot)

            if (
                !normalizedRoot.exists() ||
                !normalizedRoot.isDirectory
            ) {
                throw IOException(
                    "Extracted mod content is not a directory: " +
                            normalizedRoot.absolutePath
                )
            }

            fileCopier(
                normalizedRoot,
                stagedDir,
                cancellationSignal
            )

            cancellationSignal
                .throwIfCancellationRequested()

            return directoryReplacer.replace(
                stagedDir = stagedDir,
                finalDir = finalDir
            )
        } catch (exception: Exception) {
            val stagingCleanupFailure =
                cleanFailedStaging(stagedDir)

            if (
                exception is
                        InstallCancelledException
            ) {
                stagingCleanupFailure?.let(
                    exception::addSuppressed
                )
                throw exception
            }

            val installFailure = IOException(
                "Archive install failed for " +
                        "${archive.name}: " +
                        exception.message,
                exception
            )

            stagingCleanupFailure?.let(
                installFailure::addSuppressed
            )

            throw installFailure
        } finally {
            if (
                extractFolder.exists() &&
                !extractFolder.deleteRecursively()
            ) {
                debugLog(
                    "Could not clean direct-install " +
                            "session: " +
                            extractFolder.absolutePath
                )
            }
        }
    }

    private fun cleanFailedStaging(
        stagedDir: File
    ): IOException? {
        if (!stagedDir.exists()) {
            return null
        }

        return try {
            if (stagedDir.deleteRecursively()) {
                null
            } else {
                IOException(
                    "Could not clean the failed staged " +
                            "install: " +
                            stagedDir.absolutePath
                )
            }
        } catch (exception: Exception) {
            IOException(
                "Could not clean the failed staged " +
                        "install: " +
                        stagedDir.absolutePath,
                exception
            )
        }
    }

    private fun ensureDirectoryExists(
        directory: File,
        description: String
    ) {
        if (
            !directory.exists() &&
            !directory.mkdirs()
        ) {
            throw IOException(
                "Could not create $description: " +
                        directory.absolutePath
            )
        }

        if (!directory.isDirectory) {
            throw IOException(
                "Configured $description is not " +
                        "a directory: " +
                        directory.absolutePath
            )
        }
    }

    private fun normalizeExtractedStructure(
        root: File
    ): File {
        val children =
            root.listFiles() ?: return root

        if (
            children.size == 1 &&
            children[0].isDirectory
        ) {
            val child = children[0]
            val dataFolder =
                File(child, "Data")

            if (dataFolder.exists()) {
                return dataFolder
            }

            return child
        }

        val dataFolder =
            File(root, "Data")

        if (dataFolder.exists()) {
            return dataFolder
        }

        return root
    }
}
