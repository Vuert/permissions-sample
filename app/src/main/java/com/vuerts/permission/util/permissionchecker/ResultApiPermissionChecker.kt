package com.vuerts.permission.util.permissionchecker

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

private typealias PermissionResult = Map<String, Boolean>
private typealias ResultCallback = (PermissionResult) -> Unit
private typealias ActivityWeakRef = WeakReference<PermissionCheckerActivity>

private const val ACTIVITY_CHECK_TIMEOUT_MS = 500L
private const val ACTIVITY_CHECK_ATTEMPTS_AMOUNT = 3

class ResultApiPermissionChecker(
    private val mainDispatcher: CoroutineContext = Dispatchers.Main.immediate,
) : PermissionChecker {

    private val mutex = Mutex()

    private var activityWeakRef: ActivityWeakRef? = null

    private var resultCallback: ResultCallback? = null

    /**
     * Stores [activity] using a [WeakReference]. Call it on [Activity.onStart]
     */
    @MainThread
    fun attach(activity: PermissionCheckerActivity) {
        activityWeakRef = WeakReference(activity)
    }

    /**
     * Accepts permission result from an activity
     */
    @MainThread
    fun onPermissionResult(result: PermissionResult) {
        resultCallback?.invoke(result)
    }

    /**
     * @see PermissionChecker.checkPermissions
     */
    override suspend fun checkPermissions(vararg permissions: String): Result<Unit> {
        mutex.lock()

        return try {
            val result: PermissionResult = withContext(mainDispatcher) {
                var activity: PermissionCheckerActivity? = awaitForActivityOrThrow()

                val result: PermissionResult = suspendCancellableCoroutine {
                    resultCallback = it::resume
                    it.invokeOnCancellation { resultCallback = null }
                    activity?.resultLauncher?.launch(arrayOf(*permissions))
                    activity = null // Preventing memory leak
                }

                resultCallback = null

                result
            }

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
            activityWeakRef?.get()?.isDestroyed != false &&
            checkAttempts != ACTIVITY_CHECK_ATTEMPTS_AMOUNT
        ) {
            delay(ACTIVITY_CHECK_TIMEOUT_MS)
            checkAttempts++
        }

        return activityWeakRef?.get().let {
            coroutineContext.ensureActive()
            if (it?.isDestroyed == false) {
                it
            } else {
                throw ActivityNotFoundException()
            }
        }
    }
}
