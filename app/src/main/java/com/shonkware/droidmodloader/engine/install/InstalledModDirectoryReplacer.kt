package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class InstalledModDirectoryReplacer internal constructor(
    private val operations: InstalledModDirectoryOperations,
    private val replacementId: () -> String,
    private val cleanupWarning: (String) -> Unit
) {
    constructor() : this(
        operations = DefaultInstalledModDirectoryOperations,
        replacementId = { System.currentTimeMillis().toString() },
        cleanupWarning = {}
    )

    fun replace(
        stagedDir: File,
        finalDir: File
    ): File {
        requireValidPaths(stagedDir, finalDir)

        val backupDir = File(
            requireNotNull(finalDir.parentFile),
            "_dml_backup_${finalDir.name}_${replacementId()}"
        )

        if (backupDir.exists()) {
            throw IOException(
                "Replacement backup already exists: ${backupDir.absolutePath}"
            )
        }

        val hadExistingInstall = finalDir.exists()

        if (hadExistingInstall) {
            try {
                operations.move(finalDir, backupDir)
            } catch (exception: Exception) {
                throw IOException(
                    "Could not preserve the existing installed mod before replacement.",
                    exception
                )
            }
        }

        try {
            operations.move(stagedDir, finalDir)
        } catch (promotionFailure: Exception) {
            if (!hadExistingInstall) {
                throw IOException(
                    "Could not promote the staged mod installation.",
                    promotionFailure
                )
            }

            restoreBackupAfterPromotionFailure(
                finalDir = finalDir,
                backupDir = backupDir,
                promotionFailure = promotionFailure
            )
        }

        cleanupBackupAfterSuccessfulReplacement(backupDir)

        return finalDir
    }

    private fun requireValidPaths(
        stagedDir: File,
        finalDir: File
    ) {
        if (!stagedDir.exists() || !stagedDir.isDirectory) {
            throw IOException(
                "Staged mod directory does not exist: ${stagedDir.absolutePath}"
            )
        }

        val stagedParent = stagedDir.parentFile?.canonicalFile
            ?: throw IOException("Staged mod directory has no parent.")

        val finalParent = finalDir.parentFile?.canonicalFile
            ?: throw IOException("Final mod directory has no parent.")

        if (stagedParent != finalParent) {
            throw IOException(
                "Staged and final mod directories must have the same parent."
            )
        }

        if (!finalParent.exists() && !finalParent.mkdirs()) {
            throw IOException(
                "Could not create the installed-mod directory: ${finalParent.absolutePath}"
            )
        }

        if (!finalParent.isDirectory) {
            throw IOException(
                "Installed-mod path is not a directory: ${finalParent.absolutePath}"
            )
        }

        if (stagedDir.canonicalFile == finalDir.canonicalFile) {
            throw IOException(
                "Staged and final mod directories must be different."
            )
        }
    }

    private fun restoreBackupAfterPromotionFailure(
        finalDir: File,
        backupDir: File,
        promotionFailure: Exception
    ): Nothing {
        val rollbackFailures = mutableListOf<Exception>()

        if (finalDir.exists()) {
            try {
                if (!operations.deleteRecursively(finalDir)) {
                    rollbackFailures += IOException(
                        "Could not remove the incomplete replacement directory."
                    )
                }
            } catch (exception: Exception) {
                rollbackFailures += exception
            }
        }

        try {
            if (!backupDir.exists()) {
                rollbackFailures += IOException(
                    "The replacement backup is missing."
                )
            } else {
                operations.move(backupDir, finalDir)
            }
        } catch (exception: Exception) {
            rollbackFailures += exception
        }

        if (rollbackFailures.isNotEmpty()) {
            val exception = IOException(
                "Could not promote the staged mod and rollback was incomplete. " +
                        "Recoverable content may remain at ${backupDir.absolutePath}.",
                promotionFailure
            )

            rollbackFailures.forEach(exception::addSuppressed)
            throw exception
        }

        throw IOException(
            "Could not promote the staged mod. The prior installation was restored.",
            promotionFailure
        )
    }

    private fun cleanupBackupAfterSuccessfulReplacement(
        backupDir: File
    ) {
        if (!backupDir.exists()) {
            return
        }

        val warning = try {
            if (operations.deleteRecursively(backupDir)) {
                null
            } else {
                "The replacement succeeded, but the backup could not be removed: " +
                        backupDir.absolutePath
            }
        } catch (exception: Exception) {
            "The replacement succeeded, but backup cleanup failed: " +
                    "${backupDir.absolutePath}: ${exception.message}"
        }

        if (warning != null) {
            runCatching {
                cleanupWarning(warning)
            }
        }
    }
}

internal interface InstalledModDirectoryOperations {
    fun move(
        source: File,
        target: File
    )

    fun deleteRecursively(target: File): Boolean
}

internal object DefaultInstalledModDirectoryOperations :
    InstalledModDirectoryOperations {

    override fun move(
        source: File,
        target: File
    ) {
        target.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                throw IOException(
                    "Could not create directory: ${parent.absolutePath}"
                )
            }
        }

        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath()
            )
        }
    }

    override fun deleteRecursively(target: File): Boolean {
        return target.deleteRecursively()
    }
}