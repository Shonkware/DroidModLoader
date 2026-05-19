package com.shonkware.droidmodloader.engine.data

import com.shonkware.droidmodloader.engine.model.InstalledModRecord
import org.json.JSONObject
import java.io.File

class InstalledModRecordRepository {

    companion object {
        private const val RECORD_FILE_NAME = ".dml_mod.json"
    }

    fun getRecordFile(modDir: File): File {
        return File(modDir, RECORD_FILE_NAME)
    }

    fun saveRecord(modDir: File, record: InstalledModRecord) {
        val json = JSONObject().apply {
            put("modId", record.modId)
            put("displayName", record.displayName)
            put("installPath", record.installPath)
            put("sourceType", record.sourceType)

            if (record.sourceArchiveName == null) {
                put("sourceArchiveName", JSONObject.NULL)
            } else {
                put("sourceArchiveName", record.sourceArchiveName)
            }

            put("installedAtEpochMillis", record.installedAtEpochMillis)

            putNullableString("nexusGameDomain", record.nexusGameDomain)
            putNullableLong("nexusModId", record.nexusModId)
            putNullableLong("nexusFileId", record.nexusFileId)
            putNullableString("nexusFileName", record.nexusFileName)
            putNullableString("version", record.version)
            putNullableString("sourceUrl", record.sourceUrl)
        }

        val recordFile = getRecordFile(modDir)
        recordFile.parentFile?.mkdirs()
        recordFile.writeText(json.toString(2))
    }

    fun loadRecord(modDir: File): InstalledModRecord? {
        val file = getRecordFile(modDir)
        if (!file.exists()) return null

        val text = file.readText()
        if (text.isBlank()) return null

        val json = JSONObject(text)

        return InstalledModRecord(
            modId = json.optString("modId", modDir.name),
            displayName = json.optString("displayName", modDir.name),
            installPath = json.optString("installPath", modDir.absolutePath),
            sourceType = json.optString("sourceType", "unknown"),
            sourceArchiveName = json.optNullableString("sourceArchiveName"),
            installedAtEpochMillis = json.optLong("installedAtEpochMillis", 0L),

            nexusGameDomain = json.optNullableString("nexusGameDomain"),
            nexusModId = json.optNullableLong("nexusModId"),
            nexusFileId = json.optNullableLong("nexusFileId"),
            nexusFileName = json.optNullableString("nexusFileName"),
            version = json.optNullableString("version"),
            sourceUrl = json.optNullableString("sourceUrl")
        )
    }

    private fun JSONObject.putNullableString(name: String, value: String?) {
        if (value.isNullOrBlank()) {
            put(name, JSONObject.NULL)
        } else {
            put(name, value)
        }
    }

    private fun JSONObject.putNullableLong(name: String, value: Long?) {
        if (value == null) {
            put(name, JSONObject.NULL)
        } else {
            put(name, value)
        }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null

        return optString(name)
            .takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        if (!has(name) || isNull(name)) return null
        return optLong(name)
    }
}