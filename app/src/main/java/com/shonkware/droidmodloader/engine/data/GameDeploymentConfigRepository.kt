package com.shonkware.droidmodloader.engine.data

import com.shonkware.droidmodloader.engine.model.GameDeploymentConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class GameDeploymentConfigRepository(
    private val configFile: File
) {

    fun save(configs: List<GameDeploymentConfig>) {
        val array = JSONArray()

        for (config in configs) {
            val obj = JSONObject()
            obj.put("gameId", config.gameId)
            obj.put("displayName", config.displayName)
            obj.put("targetDataPath", config.targetDataPath)
            obj.put("realDeployEnabled", config.realDeployEnabled)
            obj.put("targetTreeUri", config.targetTreeUri)
            array.put(obj)
        }

        configFile.parentFile?.mkdirs()
        configFile.writeText(array.toString(2))
    }

    fun load(): List<GameDeploymentConfig> {
        if (!configFile.exists()) return emptyList()

        val text = configFile.readText()
        if (text.isBlank()) return emptyList()

        val array = JSONArray(text)
        val results = mutableListOf<GameDeploymentConfig>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            results.add(
                GameDeploymentConfig(
                    gameId = obj.optString("gameId", ""),
                    displayName = obj.optString("displayName", ""),
                    targetDataPath = obj.optString("targetDataPath", ""),
                    realDeployEnabled = obj.optBoolean("realDeployEnabled", false),
                    targetTreeUri = if (obj.isNull("targetTreeUri")) null else obj.optString("targetTreeUri", null)
                )
            )
        }

        return results
    }
}