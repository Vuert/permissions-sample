package com.vuerts.permission.util.permissionchecker

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vuerts.permission.util.extensions.lifecycle.launchOnLifecycleStart
import org.koin.android.ext.android.get

/**
 * Base activity for all activities.
 * Attaches to [ResultApiPermissionChecker] and transfers permission result
 */
abstract class PermissionCheckerActivity : AppCompatActivity() {

    private val permissionChecker = get<ResultApiPermissionChecker>()

    val resultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result: Map<String, Boolean> ->
        permissionChecker.onPermissionResult(result)
        onPermissionResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchOnLifecycleStart {
            permissionChecker.attach(this@PermissionCheckerActivity)
        }
    }

    open fun onPermissionResult(result: Map<String, Boolean>) {
        // None
    }
}
