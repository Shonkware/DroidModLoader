package com.shonkware.droidmodloader.engine.data

import java.io.File

class PluginOutputRepository(
    private val pluginsTxtFile: File,
    private val loadorderTxtFile: File
) {

    fun savePluginsTxt(content: String) {
        pluginsTxtFile.parentFile?.mkdirs()
        pluginsTxtFile.writeText(content)
    }

    fun saveLoadorderTxt(content: String) {
        loadorderTxtFile.parentFile?.mkdirs()
        loadorderTxtFile.writeText(content)
    }

    fun readPluginsTxt(): String {
        if (!pluginsTxtFile.exists()) return ""
        return pluginsTxtFile.readText()
    }

    fun readLoadorderTxt(): String {
        if (!loadorderTxtFile.exists()) return ""
        return loadorderTxtFile.readText()
    }
}