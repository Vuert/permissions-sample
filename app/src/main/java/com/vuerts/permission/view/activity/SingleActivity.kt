package com.vuerts.permission.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vuerts.permission.databinding.ActivitySingleBinding
import com.vuerts.permission.util.permissionchecker.ResultApiPermissionChecker
import org.koin.android.ext.android.get

class SingleActivity : AppCompatActivity() {

    // Keep strong reference in activity
    private val launcher = get<ResultApiPermissionChecker>().attach(this)

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivitySingleBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
