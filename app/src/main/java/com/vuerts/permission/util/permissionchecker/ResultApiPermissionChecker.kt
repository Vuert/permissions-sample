package com.vuerts.permission.util.permissionchecker

import android.content.ActivityNotFoundException
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

private typealias PermissionResult = Map<String, Boolean>
private typealias ResultCallback = (PermissionResult) -> Unit

private const val ACTIVITY_CHECK_TIMEOUT_MS = 500L
private const val ACTIVITY_CHECK_ATTEMPTS_AMOUNT = 3

class ResultApiPermissionChecker(
    private val activityProvider: PermissionActivityProvider,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : PermissionChecker {

    private val mutex = Mutex()

    private var resultCallback: ResultCallback? = null

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

        var activity: PermissionCheckerActivity? = null

        while (coroutineContext.isActive &&
            activityProvider.provide().also { activity = it }?.isDestroyed != false &&
            checkAttempts != ACTIVITY_CHECK_ATTEMPTS_AMOUNT
        ) {
            activity = null
            delay(ACTIVITY_CHECK_TIMEOUT_MS)
            checkAttempts++
        }

        return activity.let {
            coroutineContext.ensureActive()
            if (activity?.isDestroyed != false) {
                throw ActivityNotFoundException()
            } else {
                requireNotNull(activity)
            }
        }
    }
}
