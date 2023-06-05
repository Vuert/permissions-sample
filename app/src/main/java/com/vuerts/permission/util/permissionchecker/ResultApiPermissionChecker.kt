package com.vuerts.permission.util.permissionchecker

import android.app.Activity
import android.content.Context
import androidx.annotation.MainThread
import com.vuerts.permission.util.extensions.content.isPermissionDenied
import com.vuerts.permission.util.extensions.lifecycle.isAtLeastStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

private typealias PermissionResult = Map<String, Boolean>
private typealias ResultCallback = (PermissionResult) -> Unit
private typealias ActivityWeakRef = WeakReference<PermissionCheckerActivity>
private typealias ActivityCallback = (PermissionCheckerActivity) -> Unit

class ResultApiPermissionChecker(
    private val context: Context,
    private val mainDispatcher: CoroutineContext = Dispatchers.Main.immediate,
) : PermissionChecker {

    private val mutex = Mutex()

    private var activityWeakRef: ActivityWeakRef? = null

    private var resultCallback: ResultCallback? = null

    private var onNewActivityCallback: ActivityCallback? = null

    /**
     * Stores [activity] using a [WeakReference]. Call it on [Activity.onStart]
     */
    @MainThread
    fun attach(activity: PermissionCheckerActivity) {
        activityWeakRef = WeakReference(activity)
        onNewActivityCallback?.invoke(activity)
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
    override suspend fun checkPermissions(vararg permissions: String): Result<Unit> =
        if (permissions.any(context::isPermissionDenied)) {

            mutex.lock()
            try {
                val result: PermissionResult = withContext(mainDispatcher) {
                    var activity: PermissionCheckerActivity? = awaitForStartedActivity()

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
        } else {
            Result.success(Unit)
        }

    /**
     * Awaits for attached and started activity
     */
    private suspend fun awaitForStartedActivity(): PermissionCheckerActivity {
        val activity = activityWeakRef?.get()

        return if (activity?.isAtLeastStarted == true) {
            activity
        } else {
            val newActivity = suspendCancellableCoroutine {
                it.invokeOnCancellation { onNewActivityCallback = null }
                onNewActivityCallback = it::resume
            }
            onNewActivityCallback = null

            newActivity
        }
    }
}
