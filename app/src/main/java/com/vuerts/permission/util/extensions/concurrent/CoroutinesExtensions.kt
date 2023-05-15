package com.vuerts.permission.util.extensions.concurrent

import kotlinx.coroutines.CancellationException

/**
 * Awaits for coroutine cancellation invokes [block] on cancellation
 */
suspend inline fun awaitCancellation(logger: (Throwable) -> Unit = {}, block: () -> Unit) {
    try {
        kotlinx.coroutines.awaitCancellation()
    } catch (_: CancellationException) {
        block.invoke()
    } catch (throwable: Throwable) {
        logger.invoke(throwable)
    }
}
