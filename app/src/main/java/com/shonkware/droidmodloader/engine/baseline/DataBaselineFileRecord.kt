package com.shonkware.droidmodloader.engine.baseline

data class DataBaselineFileRecord(
    val normalizedPath: String,
    val sizeBytes: Long?,
    val modifiedEpochMillis: Long?
)