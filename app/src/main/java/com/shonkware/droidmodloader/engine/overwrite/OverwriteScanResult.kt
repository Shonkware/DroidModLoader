package com.shonkware.droidmodloader.engine.overwrite

data class OverwriteScanResult(
    val baselineExists: Boolean,
    val entries: List<OverwriteEntry>,
    val message: String
)