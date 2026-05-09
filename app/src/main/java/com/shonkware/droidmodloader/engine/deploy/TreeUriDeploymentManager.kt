package com.shonkware.droidmodloader.engine.deploy

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.shonkware.droidmodloader.engine.build.DiffEngine
import com.shonkware.droidmodloader.engine.build.FileChange
import com.shonkware.droidmodloader.engine.model.DeploymentRecord
import com.shonkware.droidmodloader.engine.model.FileRecord
import java.io.File

class TreeUriDeploymentManager(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val treeUri: Uri
) {

    fun deploy(
        oldRecords: List<DeploymentRecord>,
        newFileRecords: List<FileRecord>
    ): Pair<List<DeploymentRecord>, DeploymentResult> {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Could not open deployment tree URI.")

        val oldFileRecords = oldRecords.map {
            FileRecord(
                normalizedPath = it.normalizedPath,
                winningModId = it.winningModId,
                winningModName = it.winningModId,
                sourceFilePath = it.sourceFilePath,
                hash = it.hash
            )
        }

        val diffEngine = DiffEngine()
        val changes = diffEngine.diff(oldFileRecords, newFileRecords)

        for (change in changes) {
            when (change) {
                is FileChange.Add -> {
                    writeFileIntoTree(root, change.record)
                }
                is FileChange.Update -> {
                    writeFileIntoTree(root, change.newRecord)
                }
                is FileChange.Remove -> {
                    deletePathFromTree(root, change.normalizedPath)
                }
            }
        }

        val newManifest = newFileRecords.map {
            DeploymentRecord(
                normalizedPath = it.normalizedPath,
                winningModId = it.winningModId,
                sourceFilePath = it.sourceFilePath,
                hash = it.hash,
                deployedRelativePath = it.normalizedPath
            )
        }

        val result = DeploymentResult(
            addCount = changes.count { it is FileChange.Add },
            removeCount = changes.count { it is FileChange.Remove },
            updateCount = changes.count { it is FileChange.Update },
            finalRecordCount = newManifest.size
        )

        return Pair(newManifest, result)
    }

    private fun writeFileIntoTree(root: DocumentFile, record: FileRecord) {
        val sourceFile = File(record.sourceFilePath)
        if (!sourceFile.exists() || !sourceFile.isFile) return

        val pathParts = record.normalizedPath.split("/").filter { it.isNotBlank() }
        if (pathParts.isEmpty()) return

        var currentDir = root
        for (i in 0 until pathParts.lastIndex) {
            val part = pathParts[i]
            currentDir = currentDir.findFile(part)
                ?: currentDir.createDirectory(part)
                        ?: throw IllegalStateException("Could not create directory '$part' in tree.")
        }

        val fileName = pathParts.last()
        currentDir.findFile(fileName)?.delete()

        val targetFile = currentDir.createFile("application/octet-stream", fileName)
            ?: throw IllegalStateException("Could not create file '$fileName' in tree.")

        contentResolver.openOutputStream(targetFile.uri)?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open output stream for '$fileName'.")
    }

    private fun deletePathFromTree(root: DocumentFile, normalizedPath: String) {
        val pathParts = normalizedPath.split("/").filter { it.isNotBlank() }
        if (pathParts.isEmpty()) return

        var current: DocumentFile = root
        for (part in pathParts) {
            val next = current.findFile(part) ?: return
            current = next
        }

        current.delete()
    }
}