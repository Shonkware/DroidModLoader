package com.shonkware.droidmodloader.engine.baseline

data class DataBaselineSnapshot(
    val gameId: String,
    val createdAtEpochMillis: Long,
    val targetDescription: String,
    val files: List<DataBaselineFileRecord>
)