package com.shonkware.droidmodloader.engine.plugins

import com.shonkware.droidmodloader.engine.data.PluginOutputRepository
import com.shonkware.droidmodloader.engine.model.PluginEntry
import java.io.File

class PluginConfigurationApplyException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

data class PluginApplicationResult(
    val gameId: String,
    val mechanism: PluginLoadOrderMechanism,
    val pluginCount: Int,
    val enabledPluginCount: Int,
    val pluginsTxtContent: String,
    val pluginsTxtPath: String,
    val loadorderTxtContent: String?,
    val loadorderTxtPath: String?,
    val timestampedPluginCount: Int,
    val timestampDataFolderPath: String?
)

class PluginConfigurationApplier(
    private val outputBuilder: PluginOutputBuilder,
    private val outputRepository: PluginOutputRepository,
    private val timestampOrderer: PluginTimestampOrderer
) {
    fun apply(
        rule: GamePluginLoadOrderRule,
        plugins: List<PluginEntry>,
        timestampDataFolder: File?
    ): PluginApplicationResult {
        val orderedPlugins = plugins.sortedBy { it.priority }
        if (orderedPlugins.isEmpty()) {
            throw PluginConfigurationApplyException(
                "No saved plugins are available to apply."
            )
        }

        val output = outputBuilder.build(orderedPlugins, rule)

        val appliedTimestampOrder = when (rule.mechanism) {
            PluginLoadOrderMechanism.TEXT_FILES -> null
            PluginLoadOrderMechanism.FILE_TIMESTAMPS -> {
                val dataFolder = timestampDataFolder
                    ?: throw PluginConfigurationApplyException(
                        "A local Data folder is required for timestamp-based plugin ordering."
                    )
                timestampOrderer.apply(dataFolder, orderedPlugins)
            }
        }

        val outputPaths = try {
            outputRepository.replaceOutputs(output)
        } catch (failure: Throwable) {
            val rollback = appliedTimestampOrder?.rollback()
            val rollbackSuffix = when {
                rollback == null -> ""
                rollback.succeeded -> " Plugin timestamps were restored."
                else -> " Timestamp rollback failed for: " +
                    "${rollback.failedPluginNames.joinToString()}."
            }

            throw PluginConfigurationApplyException(
                message = "Plugin output replacement failed.$rollbackSuffix " +
                    failure.message.orEmpty(),
                cause = failure
            )
        }

        return PluginApplicationResult(
            gameId = rule.gameId,
            mechanism = rule.mechanism,
            pluginCount = orderedPlugins.size,
            enabledPluginCount = orderedPlugins.count { it.enabled },
            pluginsTxtContent = output.pluginsTxt,
            pluginsTxtPath = outputPaths.pluginsTxtPath,
            loadorderTxtContent = output.loadorderTxt,
            loadorderTxtPath = outputPaths.loadorderTxtPath,
            timestampedPluginCount = appliedTimestampOrder?.appliedCount ?: 0,
            timestampDataFolderPath = appliedTimestampOrder?.dataFolderPath
        )
    }
}
