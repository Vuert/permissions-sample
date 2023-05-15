package com.vuerts.permission.data.location.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.vuerts.permission.data.location.mapper.AndroidLocationToLocationMapper
import com.vuerts.permission.domain.location.exception.LocationIsOffException
import com.vuerts.permission.domain.location.model.Location
import com.vuerts.permission.domain.location.repository.LocationRepository
import com.vuerts.permission.util.permissionchecker.PermissionChecker
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.location.Location as AndroidLocation

class LocationRepositoryImpl(
    private val context: Context,
    private val permissionChecker: PermissionChecker,
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getLocation(): Location {

        val ex = permissionChecker.checkPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).exceptionOrNull()

        if ((ex is PermissionChecker.PermissionsDeniedException && ex.permissions.size > 1) ||
            (ex != null && ex !is PermissionChecker.PermissionsDeniedException)
        ) {
            throw ex
        }

        val locationService: LocationManager = context
            .getSystemService(LocationManager::class.java)

        val isLocationEnabled = locationService.run {
            isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        if (!isLocationEnabled) {
            throw LocationIsOffException()
        }

        return suspendCancellableCoroutine {
            val callback = object : LocationListener {

                override fun onLocationChanged(location: AndroidLocation) {
                    it.resume(AndroidLocationToLocationMapper().map(location))
                    locationService.removeUpdates(this)
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            it.invokeOnCancellation { locationService.removeUpdates(callback) }

            locationService.apply {
                requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0F, callback)
                requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0F, callback)
            }
        }
    }
}
