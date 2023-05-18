package com.vuerts.permission.util.permissionchecker

import android.app.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

private typealias PermissionResult = Map<String, Boolean>
private typealias ResultCallback = (PermissionResult) -> Unit
private typealias ActivityWeakRef = WeakReference<PermissionCheckerActivity>

private const val ACTIVITY_CHECK_TIMEOUT_MS = 500L
private const val ACTIVITY_CHECK_ATTEMPTS_AMOUNT = 3

class ResultApiPermissionChecker : PermissionChecker {

    private val mutex = Mutex()

    private val activityRef: AtomicReference<ActivityWeakRef?> = AtomicReference()

    private val resultCallbackRef: AtomicReference<ResultCallback?> = AtomicReference()

    /**
     * Stores [activity] using a [WeakReference]. Call it on [Activity.onStart]
     */
    fun attach(activity: PermissionCheckerActivity) {
        activityRef.set(WeakReference(activity))
    }

    /**
     * Accepts permission result from an activity
     */
    fun onPermissionResult(result: PermissionResult) {
        resultCallbackRef.get()?.invoke(result)
    }

    /**
     * @see PermissionChecker.checkPermissions
     */
    override suspend fun checkPermissions(vararg permissions: String): Result<Unit> {
        mutex.lock()

        return try {
            awaitUntilActivityAttachedAndActive()

            var resultLauncher = activityRef.get()?.get()?.resultLauncher

            val result: PermissionResult = suspendCancellableCoroutine {
                resultCallbackRef.set(it::resume)
                it.invokeOnCancellation { resultCallbackRef.set(null) }
                resultLauncher?.launch(arrayOf(*permissions))
                resultLauncher = null // Preventing memory leak
            }

            resultCallbackRef.set(null)

            if (result.all { it.value }) {
                Result.success(Unit)
            } else {
                val deniedPermissions = result
                    .entries
                    .asSequence()
                    .filter { !it.value }
                    .map { it.key }
                    .toSet()

                Result.failure(PermissionChecker.PermissionsDeniedException(deniedPermissions))
            }
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Awaits until [activityRef] is attached and active or reaches the await timeout
     */
    private suspend fun awaitUntilActivityAttachedAndActive() {
        var checkAttempts = 0

        while (coroutineContext.isActive &&
            activityRef.get()?.get()?.isDestroyed != false &&
            checkAttempts != ACTIVITY_CHECK_ATTEMPTS_AMOUNT
        ) {
            delay(ACTIVITY_CHECK_TIMEOUT_MS)
            checkAttempts++
        }

        if (activityRef.get()?.get()?.isDestroyed != false && coroutineContext.isActive) {
            throw ActivityIsNotAttachedOrActiveException()
        }
    }

    class ActivityIsNotAttachedOrActiveException : Exception(
        "Activity is not attached or active"
    )
}
