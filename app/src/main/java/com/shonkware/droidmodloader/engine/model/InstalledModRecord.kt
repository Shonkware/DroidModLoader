package com.shonkware.droidmodloader.engine.model

data class InstalledModRecord(
    val modId: String,
    val displayName: String,
    val installPath: String,
    val sourceType: String,
    val sourceArchiveName: String?,
    val installedAtEpochMillis: Long,

    val nexusGameDomain: String? = null,
    val nexusModId: Long? = null,
    val nexusFileId: Long? = null,
    val nexusFileName: String? = null,
    val version: String? = null,
    val sourceUrl: String? = null
)