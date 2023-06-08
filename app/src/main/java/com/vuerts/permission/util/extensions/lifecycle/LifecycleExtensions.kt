package com.vuerts.permission.util.extensions.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.vuerts.permission.util.extensions.concurrent.awaitCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

inline val Lifecycle.isAtLeastStarted: Boolean
    get() = currentState.isAtLeast(Lifecycle.State.STARTED)

inline fun Lifecycle.repeatOnStarted(
    crossinline block: suspend CoroutineScope.() -> Unit,
): Job = coroutineScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) { block() }
}

inline fun Lifecycle.launchOnLifecycleCreate(
    crossinline block: CoroutineScope.() -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_CREATE) { block() }

inline fun Lifecycle.launchOnLifecycleStart(
    crossinline block: CoroutineScope.() -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_START) { block() }

inline fun Lifecycle.launchOnLifecycleResume(
    crossinline block: CoroutineScope.() -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_RESUME) { block() }

inline fun Lifecycle.launchOnLifecyclePause(
    crossinline block: CoroutineScope.() -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_PAUSE) { block() }

inline fun Lifecycle.launchOnLifecycleStop(
    crossinline block: CoroutineScope.() -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_STOP) { block() }

inline fun Lifecycle.launchOnLifecycleDestroy(
    crossinline block: CoroutineScope.() -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_DESTROY) { block() }

inline fun Lifecycle.launchOnAnyLifecycleEvent(
    crossinline block: CoroutineScope.(Lifecycle.Event) -> Unit,
): Job = launchOnLifecycleEvent(Lifecycle.Event.ON_ANY, block)

inline fun Lifecycle.launchOnLifecycleEvent(
    event: Lifecycle.Event,
    crossinline block: CoroutineScope.(Lifecycle.Event) -> Unit,
): Job = coroutineScope.launch {
    val lifecycleObserver = LifecycleEventObserver { _, newEvent ->
        if (event == Lifecycle.Event.ON_ANY || event == newEvent) {
            block(newEvent)
        }
    }

    this@launchOnLifecycleEvent.addObserver(lifecycleObserver)

    awaitCancellation {
        this@launchOnLifecycleEvent.removeObserver(lifecycleObserver)
    }
}
