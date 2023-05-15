package com.vuerts.permission.util.permissionchecker

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

private typealias PermissionResult = Map<String, Boolean>
private typealias ResultLauncher = ActivityResultLauncher<Array<String>>

private const val LAUNCHER_CHECK_TIMEOUT_MS = 500L
private const val LAUNCHER_CHECK_ATTEMPTS_AMOUNT = 3

class ResultApiPermissionChecker : PermissionChecker {

    private val mutex = Mutex()

    private val resultLauncher: AtomicReference<WeakReference<ResultLauncher>> = AtomicReference()

    private val resultCallback: AtomicReference<((PermissionResult) -> Unit)?> = AtomicReference()

    private val activityResultCallback = ActivityResultCallback<PermissionResult> {
        resultCallback.get()?.invoke(it)
    }

    /**
     * Creates a result launcher from an Activity and stores it in a WeakReference
     *
     * @return Wrapped result launcher to keep strong reference in activity
     */
    @MainThread
    fun attach(activity: ComponentActivity): ResultLauncherWrapper {
        val resultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            activityResultCallback,
        )

        this.resultLauncher.set(WeakReference(resultLauncher))

        return ResultLauncherWrapper(resultLauncher)
    }

    /**
     * Checks permissions and mutex is used to order function calls, although not for synchronization
     *
     * @return [Result] either successful with unit type or with
     * [PermissionChecker.PermissionsDeniedException] that contains set of denied permissions
     */
    override suspend fun checkPermissions(vararg permissions: String): Result<Unit> {
        mutex.lock()

        return try {
            awaitWhileLauncherIsNotAttached()

            val resultLauncher = requireNotNull(resultLauncher.get().get())

            val result: PermissionResult = suspendCancellableCoroutine {
                resultCallback.set(it::resume)
                it.invokeOnCancellation { resultCallback.set(null) }
                resultLauncher.launch(arrayOf(*permissions))
            }

            resultCallback.set(null)

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
     * Awaits until the result launcher is attached or reaches the await timeout
     */
    private suspend fun awaitWhileLauncherIsNotAttached() {
        var checkAttempts = 0

        while (coroutineContext.isActive &&
            resultLauncher.get()?.get() == null &&
            checkAttempts != LAUNCHER_CHECK_ATTEMPTS_AMOUNT
        ) {
            delay(LAUNCHER_CHECK_TIMEOUT_MS)
            checkAttempts++
        }

        if (resultLauncher.get()?.get() == null && coroutineContext.isActive) {
            throw Exception("For some reason, an Activity is not attached.")
        }
    }

    /**
     * Wrapper to keep result launcher in activity
     */
    class ResultLauncherWrapper(private val launcher: ResultLauncher)
}
