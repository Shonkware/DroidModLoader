package com.shonkware.droidmodloader.engine.plugins

import com.shonkware.droidmodloader.engine.model.PluginEntry
import java.io.File

class PluginTimestampOrderException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

data class PluginTimestampRollbackResult(
    val failedPluginNames: List<String>
) {
    val succeeded: Boolean
        get() = failedPluginNames.isEmpty()
}

class AppliedPluginTimestampOrder internal constructor(
    private val changes: List<PluginTimestampChange>,
    private val timestampWriter: (File, Long) -> Boolean
) {
    val appliedCount: Int
        get() = changes.size

    val dataFolderPath: String
        get() = changes.firstOrNull()?.file?.parentFile?.absolutePath.orEmpty()

    fun rollback(): PluginTimestampRollbackResult {
        val failedNames = changes
            .asReversed()
            .mapNotNull { change ->
                val restored = runCatching {
                    timestampWriter(change.file, change.originalTimestamp)
                }.getOrDefault(false)

                change.file.name.takeUnless { restored }
            }

        return PluginTimestampRollbackResult(failedPluginNames = failedNames)
    }
}

internal data class PluginTimestampChange(
    val file: File,
    val originalTimestamp: Long,
    val appliedTimestamp: Long
)

class PluginTimestampOrderer(
    private val timestampIntervalMillis: Long = DEFAULT_TIMESTAMP_INTERVAL_MILLIS,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val timestampWriter: (File, Long) -> Boolean = { file, timestamp ->
        file.setLastModified(timestamp)
    }
) {
    init {
        require(timestampIntervalMillis > 0L) {
            "Plugin timestamp interval must be positive."
        }
    }

    fun apply(
        dataFolder: File,
        plugins: List<PluginEntry>
    ): AppliedPluginTimestampOrder {
        val orderedPlugins = plugins.sortedBy { it.priority }
        val plans = buildPlans(dataFolder, orderedPlugins)
        val appliedChanges = mutableListOf<PluginTimestampChange>()

        try {
            plans.forEach { plan ->
                val changed = timestampWriter(plan.file, plan.appliedTimestamp)
                if (!changed) {
                    throw PluginTimestampOrderException(
                        "Could not set plugin timestamp: ${plan.file.name}"
                    )
                }
                appliedChanges.add(plan)
            }

            verifyStrictlyIncreasing(appliedChanges)
        } catch (failure: Throwable) {
            val rollback = AppliedPluginTimestampOrder(
                changes = appliedChanges,
                timestampWriter = timestampWriter
            ).rollback()

            val rollbackSuffix = if (rollback.succeeded) {
                "Changed timestamps were restored."
            } else {
                "Timestamp rollback failed for: ${rollback.failedPluginNames.joinToString()}."
            }

            throw PluginTimestampOrderException(
                message = "Plugin timestamp ordering failed. $rollbackSuffix ${failure.message.orEmpty()}".trim(),
                cause = failure
            )
        }

        return AppliedPluginTimestampOrder(
            changes = appliedChanges,
            timestampWriter = timestampWriter
        )
    }

    private fun buildPlans(
        dataFolder: File,
        orderedPlugins: List<PluginEntry>
    ): List<PluginTimestampChange> {
        if (!dataFolder.exists() || !dataFolder.isDirectory) {
            throw PluginTimestampOrderException(
                "Plugin Data folder does not exist or is not a directory: ${dataFolder.absolutePath}"
            )
        }

        val duplicateRequestedNames = orderedPlugins
            .groupBy { it.pluginName.lowercase() }
            .filterValues { entries -> entries.size > 1 }
            .keys
            .sorted()

        if (duplicateRequestedNames.isNotEmpty()) {
            throw PluginTimestampOrderException(
                "Duplicate plugin names cannot be timestamp ordered: " +
                    duplicateRequestedNames.joinToString()
            )
        }

        val targetFilesByName = dataFolder.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .groupBy { it.name.lowercase() }

        val duplicateTargetNames = targetFilesByName
            .filterValues { files -> files.size > 1 }
            .keys
            .intersect(orderedPlugins.map { it.pluginName.lowercase() }.toSet())
            .sorted()

        if (duplicateTargetNames.isNotEmpty()) {
            throw PluginTimestampOrderException(
                "Data folder contains duplicate case-insensitive plugin names: " +
                    duplicateTargetNames.joinToString()
            )
        }

        val missingPluginNames = orderedPlugins
            .map { it.pluginName }
            .filter { pluginName -> targetFilesByName[pluginName.lowercase()].isNullOrEmpty() }

        if (missingPluginNames.isNotEmpty()) {
            throw PluginTimestampOrderException(
                "Plugin files are missing from the Data folder: ${missingPluginNames.joinToString()}"
            )
        }

        val unwritablePluginNames = orderedPlugins
            .mapNotNull { plugin -> targetFilesByName[plugin.pluginName.lowercase()]?.singleOrNull() }
            .filterNot { it.canWrite() }
            .map { it.name }

        if (unwritablePluginNames.isNotEmpty()) {
            throw PluginTimestampOrderException(
                "Plugin files are not writable: ${unwritablePluginNames.joinToString()}"
            )
        }

        val firstTimestamp = (
            clockMillis() - timestampIntervalMillis * orderedPlugins.size.toLong()
            ).coerceAtLeast(0L)

        return orderedPlugins.mapIndexed { index, plugin ->
            val file = targetFilesByName.getValue(plugin.pluginName.lowercase()).single()
            PluginTimestampChange(
                file = file,
                originalTimestamp = file.lastModified(),
                appliedTimestamp = firstTimestamp + timestampIntervalMillis * index.toLong()
            )
        }
    }

    private fun verifyStrictlyIncreasing(changes: List<PluginTimestampChange>) {
        val timestamps = changes.map { it.file.lastModified() }
        val invalidIndex = timestamps.zipWithNext().indexOfFirst { (current, next) ->
            current >= next
        }

        if (invalidIndex >= 0) {
            val first = changes[invalidIndex].file.name
            val second = changes[invalidIndex + 1].file.name
            throw PluginTimestampOrderException(
                "Plugin timestamps are not strictly increasing between $first and $second."
            )
        }
    }

    companion object {
        const val DEFAULT_TIMESTAMP_INTERVAL_MILLIS = 60_000L
    }
}
