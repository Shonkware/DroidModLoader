package com.shonkware.droidmodloader.engine.deploy

data class DeploymentTargetIdentity(
    val gameId: String,
    val mode: String,
    val target: String
) {
    fun stableKey(): String {
        return "$gameId|$mode|$target"
    }

    fun displaySummary(): String {
        return "game=$gameId, mode=$mode, target=$target"
    }
}