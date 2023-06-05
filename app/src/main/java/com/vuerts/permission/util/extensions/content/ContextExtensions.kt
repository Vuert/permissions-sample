package com.vuerts.permission.util.extensions.content

import android.content.Context
import android.content.pm.PackageManager

fun Context.isPermissionDenied(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED

fun Context.isPermissionGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
