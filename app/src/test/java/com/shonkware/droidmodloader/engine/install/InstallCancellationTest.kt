package com.shonkware.droidmodloader.engine.install

import org.junit.Test

class InstallCancellationTest {
    @Test
    fun `signal allows work before cancellation`() {
        val controller =
            InstallCancellationController()

        controller.signal
            .throwIfCancellationRequested()
    }

    @Test
    fun `signal throws after cancellation`() {
        val controller =
            InstallCancellationController()

        controller.cancel()

        expectCancellation {
            controller.signal
                .throwIfCancellationRequested()
        }
    }

    private fun expectCancellation(
        action: () -> Unit
    ): InstallCancelledException {
        try {
            action()
            throw AssertionError(
                "Expected InstallCancelledException"
            )
        } catch (
            exception: InstallCancelledException
        ) {
            return exception
        }
    }
}