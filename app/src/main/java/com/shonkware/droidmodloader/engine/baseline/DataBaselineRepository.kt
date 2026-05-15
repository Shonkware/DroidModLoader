package com.shonkware.droidmodloader.engine.baseline

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DataBaselineRepository(
    private val baselineFile: File
) {
    fun exists(): Boolean {
        return baselineFile.exists() && baselineFile.readText().isNotBlank()
    }

    fun load(): DataBaselineSnapshot? {
        if (!exists()) return null

        val obj = JSONObject(baselineFile.readText())
        val filesArray = obj.optJSONArray("files") ?: JSONArray()

        val files = mutableListOf<DataBaselineFileRecord>()

        for (i in 0 until filesArray.length()) {
            val fileObj = filesArray.getJSONObject(i)

            files.add(
                DataBaselineFileRecord(
                    normalizedPath = fileObj.getString("normalizedPath"),
                    sizeBytes = if (fileObj.isNull("sizeBytes")) null else fileObj.optLong("sizeBytes"),
                    modifiedEpochMillis = if (fileObj.isNull("modifiedEpochMillis")) null else fileObj.optLong("modifiedEpochMillis")
                )
            )
        }

        return DataBaselineSnapshot(
            gameId = obj.getString("gameId"),
            createdAtEpochMillis = obj.optLong("createdAtEpochMillis", 0L),
            targetDescription = obj.optString("targetDescription", ""),
            files = files
        )
    }

    fun save(snapshot: DataBaselineSnapshot) {
        val obj = JSONObject()
        obj.put("gameId", snapshot.gameId)
        obj.put("createdAtEpochMillis", snapshot.createdAtEpochMillis)
        obj.put("targetDescription", snapshot.targetDescription)

        val filesArray = JSONArray()

        snapshot.files.forEach { file ->
            val fileObj = JSONObject()
            fileObj.put("normalizedPath", file.normalizedPath)
            fileObj.put("sizeBytes", file.sizeBytes)
            fileObj.put("modifiedEpochMillis", file.modifiedEpochMillis)
            filesArray.put(fileObj)
        }

        obj.put("files", filesArray)

        baselineFile.parentFile?.mkdirs()
        baselineFile.writeText(obj.toString(2))
    }

    fun clear() {
        if (baselineFile.exists()) {
            baselineFile.delete()
        }
    }
}