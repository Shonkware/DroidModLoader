package com.shonkware.droidmodloader.engine.install

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class InstallCancelledException(
    message: String =
        "Archive installation was cancelled."
) : IOException(message)

fun interface InstallCancellationSignal {
    fun throwIfCancellationRequested()

    companion object {
        val NONE =
            InstallCancellationSignal {}
    }
}

class InstallCancellationController {
    private val cancellationRequested =
        AtomicBoolean(false)

    val signal =
        InstallCancellationSignal {
            if (cancellationRequested.get()) {
                throw InstallCancelledException()
            }
        }

    fun cancel() {
        cancellationRequested.set(true)
    }
}