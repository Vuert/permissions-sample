package com.vuerts.permission.util.permissionchecker

import android.app.Activity
import android.content.ActivityNotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
            var activity: PermissionCheckerActivity? = awaitForActivityOrThrow()

            val result: PermissionResult = suspendCancellableCoroutine {
                resultCallbackRef.set(it::resume)
                it.invokeOnCancellation { resultCallbackRef.set(null) }
                activity?.resultLauncher?.launch(arrayOf(*permissions))
                activity = null // Preventing memory leak
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
     * Awaits for attached and active activity
     *
     * @throws [ActivityNotFoundException] if reaches the timeout
     */
    private suspend fun awaitForActivityOrThrow(): PermissionCheckerActivity {
        var checkAttempts = 0

        while (coroutineContext.isActive &&
            activityRef.get()?.get()?.isDestroyed != false &&
            checkAttempts != ACTIVITY_CHECK_ATTEMPTS_AMOUNT
        ) {
            delay(ACTIVITY_CHECK_TIMEOUT_MS)
            checkAttempts++
        }

        return activityRef.get()?.get().let {
            coroutineContext.ensureActive()
            if (it?.isDestroyed == false) {
                it
            } else {
                throw ActivityNotFoundException()
            }
        }
    }
}
