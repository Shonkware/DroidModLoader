package com.shonkware.droidmodloader.engine.deploy

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.shonkware.droidmodloader.engine.model.DeploymentRecord
import com.shonkware.droidmodloader.engine.model.FileRecord
import java.io.File
import java.io.IOException

class TreeUriDeploymentManager(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val treeUri: Uri,
    private val backupRootDir: File
) {

    private data class BackupInfo(
        val backupFilePath: String,
        val backupCreatedAtEpochMillis: Long
    )

    private val root: DocumentFile by lazy {
        DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Could not open selected Tree URI target folder.")
    }

    private val directoryCache = mutableMapOf<String, DocumentFile>()
    private val childCache = mutableMapOf<String, MutableMap<String, DocumentFile>>()

    fun deploy(
        oldManifest: List<DeploymentRecord>,
        newWinningRecords: List<FileRecord>
    ): Pair<List<DeploymentRecord>, DeploymentResult> {
        directoryCache.clear()
        childCache.clear()

        directoryCache[""] = root
        backupRootDir.mkdirs()

        val oldByPath = oldManifest.associateBy { it.normalizedPath }
        val newByPath = newWinningRecords.associateBy { it.normalizedPath }

        val removes = oldByPath
            .filterKeys { path -> path !in newByPath }
            .values
            .sortedByDescending { it.normalizedPath.count { c -> c == '/' } }

        val adds = newByPath
            .filterKeys { path -> path !in oldByPath }
            .values
            .sortedBy { it.normalizedPath }

        val updates = newByPath
            .filter { (path, newRecord) ->
                val oldRecord = oldByPath[path]
                oldRecord != null && oldRecord.hash != newRecord.hash
            }
            .values
            .sortedBy { it.normalizedPath }

        var backupCount = 0
        var restoreCount = 0
        var protectedConflictCount = 0

        val changedRecordsByPath = mutableMapOf<String, DeploymentRecord>()

        for (record in removes) {
            val restored = restoreBackupOrDeleteTarget(record)
            if (restored) {
                restoreCount++
            }
        }

        for (record in updates) {
            val oldRecord = oldByPath[record.normalizedPath]

            val deployedRecord = copyRecordToTarget(
                record = record,
                oldRecord = oldRecord
            )

            if (deployedRecord.backupFilePath != null && oldRecord?.backupFilePath == null) {
                backupCount++
            }

            changedRecordsByPath[deployedRecord.normalizedPath] = deployedRecord
        }

        for (record in adds) {
            val oldRecord = oldByPath[record.normalizedPath]

            val deployedRecord = copyRecordToTarget(
                record = record,
                oldRecord = oldRecord
            )

            if (deployedRecord.backupFilePath != null && oldRecord?.backupFilePath == null) {
                backupCount++
            }

            changedRecordsByPath[deployedRecord.normalizedPath] = deployedRecord
        }

        val newManifest = newWinningRecords
            .map { fileRecord ->
                val changedRecord = changedRecordsByPath[fileRecord.normalizedPath]
                if (changedRecord != null) {
                    changedRecord
                } else {
                    fileRecord.toDeploymentRecord(
                        preservedBackupRecord = oldByPath[fileRecord.normalizedPath]
                    )
                }
            }
            .sortedBy { it.normalizedPath }

        val result = DeploymentResult(
            addCount = adds.size,
            removeCount = removes.size,
            updateCount = updates.size,
            finalRecordCount = newManifest.size,
            backupCount = backupCount,
            restoreCount = restoreCount,
            protectedConflictCount = protectedConflictCount
        )

        return Pair(newManifest, result)
    }

    private fun copyRecordToTarget(
        record: FileRecord,
        oldRecord: DeploymentRecord?
    ): DeploymentRecord {
        val sourceFile = File(record.sourceFilePath)

        if (!sourceFile.exists() || !sourceFile.isFile) {
            throw IOException("Source file missing during Tree URI deploy: ${record.sourceFilePath}")
        }

        val parentPath = getParentPath(record.normalizedPath)
        val fileName = getFileName(record.normalizedPath)
        val parentDir = getOrCreateDirectory(parentPath)

        val existing = getCachedChild(
            parentPath = parentPath,
            parentDir = parentDir,
            childName = fileName
        )

        if (existing != null && existing.isDirectory) {
            throw IOException("Target path is a directory, expected file: ${record.normalizedPath}")
        }

        val backupInfo = when {
            oldRecord?.backupFilePath != null -> {
                BackupInfo(
                    backupFilePath = oldRecord.backupFilePath,
                    backupCreatedAtEpochMillis = oldRecord.backupCreatedAtEpochMillis
                        ?: System.currentTimeMillis()
                )
            }

            oldRecord != null -> {
                null
            }

            existing != null && existing.isFile -> {
                backupExistingTargetFile(
                    normalizedPath = record.normalizedPath,
                    targetFile = existing
                )
            }

            else -> {
                null
            }
        }

        if (existing != null) {
            existing.delete()
            removeCachedChild(parentPath, fileName)
        }

        val targetFile = parentDir.createFile(
            guessMimeType(fileName),
            fileName
        ) ?: throw IOException("Could not create target file through Tree URI: ${record.normalizedPath}")

        sourceFile.inputStream().use { input ->
            contentResolver.openOutputStream(targetFile.uri, "w").use { output ->
                if (output == null) {
                    throw IOException("Could not open target output stream: ${record.normalizedPath}")
                }

                input.copyTo(output, bufferSize = 256 * 1024)
            }
        }

        putCachedChild(parentPath, fileName, targetFile)

        return record.toDeploymentRecord(
            preservedBackupRecord = oldRecord,
            newBackupInfo = backupInfo
        )
    }

    private fun backupExistingTargetFile(
        normalizedPath: String,
        targetFile: DocumentFile
    ): BackupInfo {
        val backupFile = resolveInsideBackupRoot(normalizedPath)
        backupFile.parentFile?.mkdirs()

        contentResolver.openInputStream(targetFile.uri).use { input ->
            if (input == null) {
                throw IOException("Could not open existing target file for backup: $normalizedPath")
            }

            backupFile.outputStream().use { output ->
                input.copyTo(output, bufferSize = 256 * 1024)
            }
        }

        return BackupInfo(
            backupFilePath = backupFile.absolutePath,
            backupCreatedAtEpochMillis = System.currentTimeMillis()
        )
    }

    private fun restoreBackupOrDeleteTarget(record: DeploymentRecord): Boolean {
        val parentPath = getParentPath(record.normalizedPath)
        val fileName = getFileName(record.normalizedPath)

        val parentDir = getExistingDirectory(parentPath)

        val existing = if (parentDir != null) {
            getCachedChild(
                parentPath = parentPath,
                parentDir = parentDir,
                childName = fileName
            )
        } else {
            null
        }

        val backupPath = record.backupFilePath

        if (backupPath.isNullOrBlank()) {
            if (existing != null && existing.isFile) {
                existing.delete()
                removeCachedChild(parentPath, fileName)
            }

            return false
        }

        val backupFile = File(backupPath)
        if (!backupFile.exists() || !backupFile.isFile) {
            throw IOException(
                "Backup file missing; refusing to delete Tree URI target without restore: ${record.normalizedPath}"
            )
        }

        if (existing != null) {
            if (existing.isDirectory) {
                throw IOException("Target path is a directory, expected file during restore: ${record.normalizedPath}")
            }

            existing.delete()
            removeCachedChild(parentPath, fileName)
        }

        val restoreParentDir = getOrCreateDirectory(parentPath)

        val restoredFile = restoreParentDir.createFile(
            guessMimeType(fileName),
            fileName
        ) ?: throw IOException("Could not recreate target file for backup restore: ${record.normalizedPath}")

        backupFile.inputStream().use { input ->
            contentResolver.openOutputStream(restoredFile.uri, "w").use { output ->
                if (output == null) {
                    throw IOException("Could not open target output stream for backup restore: ${record.normalizedPath}")
                }

                input.copyTo(output, bufferSize = 256 * 1024)
            }
        }

        putCachedChild(parentPath, fileName, restoredFile)

        backupFile.delete()
        cleanupEmptyBackupParents(backupFile)

        return true
    }

    private fun getOrCreateDirectory(path: String): DocumentFile {
        if (path.isBlank()) {
            return root
        }

        val cached = directoryCache[path]
        if (cached != null) {
            return cached
        }

        var currentPath = ""
        var currentDir = root
        var parentPath = ""

        val parts = path
            .split("/")
            .filter { it.isNotBlank() }

        for (part in parts) {
            currentPath = if (currentPath.isBlank()) {
                part
            } else {
                "$currentPath/$part"
            }

            val cachedDir = directoryCache[currentPath]
            if (cachedDir != null) {
                currentDir = cachedDir
                parentPath = currentPath
                continue
            }

            val child = getCachedChild(
                parentPath = parentPath,
                parentDir = currentDir,
                childName = part
            )

            val nextDir = when {
                child != null && child.isDirectory -> child

                child != null && child.isFile -> {
                    throw IOException("Target path segment is a file, expected directory: $currentPath")
                }

                else -> {
                    val created = currentDir.createDirectory(part)
                        ?: throw IOException("Could not create target directory through Tree URI: $currentPath")

                    putCachedChild(parentPath, part, created)
                    created
                }
            }

            directoryCache[currentPath] = nextDir
            currentDir = nextDir
            parentPath = currentPath
        }

        return currentDir
    }

    private fun getExistingDirectory(path: String): DocumentFile? {
        if (path.isBlank()) {
            return root
        }

        val cached = directoryCache[path]
        if (cached != null) {
            return cached
        }

        var currentPath = ""
        var currentDir = root
        var parentPath = ""

        val parts = path
            .split("/")
            .filter { it.isNotBlank() }

        for (part in parts) {
            currentPath = if (currentPath.isBlank()) {
                part
            } else {
                "$currentPath/$part"
            }

            val cachedDir = directoryCache[currentPath]
            if (cachedDir != null) {
                currentDir = cachedDir
                parentPath = currentPath
                continue
            }

            val child = getCachedChild(
                parentPath = parentPath,
                parentDir = currentDir,
                childName = part
            )

            if (child == null || !child.isDirectory) {
                return null
            }

            directoryCache[currentPath] = child
            currentDir = child
            parentPath = currentPath
        }

        return currentDir
    }

    private fun childLookupKey(name: String): String {
        return name.lowercase()
    }

    private fun getCachedChild(
        parentPath: String,
        parentDir: DocumentFile,
        childName: String
    ): DocumentFile? {
        val children = getCachedChildren(parentPath, parentDir)
        return children[childLookupKey(childName)]
    }

    private fun getCachedChildren(
        parentPath: String,
        parentDir: DocumentFile
    ): MutableMap<String, DocumentFile> {
        val cached = childCache[parentPath]
        if (cached != null) {
            return cached
        }

        val loaded = mutableMapOf<String, DocumentFile>()

        parentDir.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            val key = childLookupKey(name)

            if (!loaded.containsKey(key)) {
                loaded[key] = child
            }
        }

        childCache[parentPath] = loaded
        return loaded
    }

    private fun putCachedChild(
        parentPath: String,
        childName: String,
        child: DocumentFile
    ) {
        val children = childCache[parentPath]
        if (children != null) {
            children[childLookupKey(childName)] = child
        }
    }

    private fun removeCachedChild(
        parentPath: String,
        childName: String
    ) {
        childCache[parentPath]?.remove(childLookupKey(childName))
    }

    private fun getParentPath(normalizedPath: String): String {
        return normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
    }

    private fun getFileName(normalizedPath: String): String {
        return normalizedPath.substringAfterLast("/")
    }

    private fun resolveInsideBackupRoot(normalizedPath: String): File {
        val target = File(backupRootDir, normalizedPath)
        val rootPath = backupRootDir.canonicalPath + File.separator
        val targetPath = target.canonicalPath

        if (!targetPath.startsWith(rootPath)) {
            throw SecurityException("Blocked unsafe Tree URI backup path: $normalizedPath")
        }

        return target
    }

    private fun cleanupEmptyBackupParents(startFile: File) {
        val rootCanonical = backupRootDir.canonicalFile
        var current = startFile.parentFile?.canonicalFile

        while (current != null) {
            if (current == rootCanonical) return
            if (!current.canonicalPath.startsWith(rootCanonical.canonicalPath + File.separator)) return

            val children = current.listFiles()
            if (children != null && children.isNotEmpty()) return

            val parent = current.parentFile?.canonicalFile
            current.delete()
            current = parent
        }
    }

    private fun guessMimeType(fileName: String): String {
        return "application/octet-stream"
    }

    private fun FileRecord.toDeploymentRecord(
        preservedBackupRecord: DeploymentRecord?,
        newBackupInfo: BackupInfo? = null
    ): DeploymentRecord {
        val backupFilePath = newBackupInfo?.backupFilePath
            ?: preservedBackupRecord?.backupFilePath

        val backupCreatedAt = newBackupInfo?.backupCreatedAtEpochMillis
            ?: preservedBackupRecord?.backupCreatedAtEpochMillis

        return DeploymentRecord(
            normalizedPath = normalizedPath,
            sourceFilePath = sourceFilePath,
            winningModId = winningModId,
            winningModName = winningModName,
            hash = hash,
            hadPreExistingTargetFile = backupFilePath != null ||
                    preservedBackupRecord?.hadPreExistingTargetFile == true,
            backupFilePath = backupFilePath,
            backupCreatedAtEpochMillis = backupCreatedAt
        )
    }
}