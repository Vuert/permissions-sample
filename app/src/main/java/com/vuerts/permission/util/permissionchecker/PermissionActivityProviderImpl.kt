package com.vuerts.permission.util.permissionchecker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.vuerts.permission.util.lifecycle.ActivityLifecycleCallbacks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.WeakHashMap

typealias PermissionActivityRef = WeakReference<PermissionCheckerActivity>

class PermissionActivityProviderImpl(
    application: Application,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : PermissionActivityProvider {

    private var currentActivity: PermissionActivityRef? = null

    private val activitiesMap = WeakHashMap<String, PermissionActivityRef>()

    init {
        application.registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (activity is PermissionCheckerActivity) {
                        activitiesMap[activity.key] = WeakReference(activity)
                    }
                }
            },
        )
    }

    override suspend fun provide(): PermissionCheckerActivity? =
        withContext(mainDispatcher) {
            if (currentActivity?.get()?.isDestroyed == false) {
                currentActivity?.get()
            } else {
                activitiesMap
                    .values
                    .firstOrNull { it?.get()?.isDestroyed == false }
                    ?.also(::currentActivity::set)
                    ?.get()
            }
        }
}
