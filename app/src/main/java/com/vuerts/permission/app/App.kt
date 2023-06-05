package com.vuerts.permission.app

import android.app.Application
import com.vuerts.permission.data.location.repository.LocationRepositoryImpl
import com.vuerts.permission.domain.location.repository.LocationRepository
import com.vuerts.permission.presentation.location.viewmodel.LocationViewModel
import com.vuerts.permission.util.permissionchecker.PermissionChecker
import com.vuerts.permission.util.permissionchecker.ResultApiPermissionChecker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        initDI()
    }

    private fun initDI() {
        startKoin {
            androidContext(this@App)
            modules(getModule())
        }
    }
}

private fun getModule(): Module = module {
    single {
        ResultApiPermissionChecker(context = androidContext())
    } bind PermissionChecker::class

    single<LocationRepository> {
        LocationRepositoryImpl(
            context = androidContext(),
            permissionChecker = get(),
        )
    }

    viewModel { LocationViewModel(locationRepository = get()) }
}
