package com.vuerts.permission.view.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.vuerts.permission.databinding.ActivitySingleBinding
import com.vuerts.permission.util.permissionchecker.PermissionCheckerActivity

class SingleActivity : PermissionCheckerActivity() {

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivitySingleBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
