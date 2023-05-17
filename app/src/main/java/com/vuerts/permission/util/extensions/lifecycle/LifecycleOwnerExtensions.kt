package com.vuerts.permission.util.extensions.lifecycle

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

inline fun LifecycleOwner.repeatOnStarted(
    crossinline block: suspend CoroutineScope.() -> Unit,
): Job = lifecycle.repeatOnStarted(block)

inline fun LifecycleOwner.launchOnLifecycleStart(
    crossinline block: CoroutineScope.() -> Unit,
): Job = lifecycle.launchOnLifecycleStart(block)

inline fun LifecycleOwner.launchOnLifecycleDestroy(
    crossinline block: CoroutineScope.() -> Unit,
): Job = lifecycle.launchOnLifecycleDestroy(block)
