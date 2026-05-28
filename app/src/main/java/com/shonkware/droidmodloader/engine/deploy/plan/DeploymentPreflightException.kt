package com.shonkware.droidmodloader.engine.deploy.plan

class DeploymentPreflightException(
    val result: DeploymentPreflightResult
) : IllegalStateException(result.toDebugSummary())