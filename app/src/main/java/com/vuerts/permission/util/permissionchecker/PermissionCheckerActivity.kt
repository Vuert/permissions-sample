package com.vuerts.permission.util.permissionchecker

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.get
import java.util.UUID
import kotlin.properties.Delegates

/**
 * Base activity for all activities. Transfers permission result to [ResultApiPermissionChecker]
 */
abstract class PermissionCheckerActivity : AppCompatActivity() {

    private val permissionChecker = get<ResultApiPermissionChecker>()

    /**
     * Unique activity key
     */
    var key: String by Delegates.notNull()
        private set

    val resultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result: Map<String, Boolean> ->
        permissionChecker.onPermissionResult(result)
        onPermissionResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initKey(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    private fun initKey(savedInstanceState: Bundle?) {
        key = savedInstanceState?.getString(ACTIVITY_KEY)
            ?: "${UUID.randomUUID()}:${System.currentTimeMillis()}"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ACTIVITY_KEY, key)
        super.onSaveInstanceState(outState)
    }

    open fun onPermissionResult(result: Map<String, Boolean>) {
        // None
    }

    companion object {
        private const val ACTIVITY_KEY = "ACTIVITY_KEY"
    }
}
