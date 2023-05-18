package com.vuerts.permission.util.permissionchecker

/**
 * Provides the first active [PermissionCheckerActivity]
 */
interface PermissionActivityProvider {

    suspend fun provide(): PermissionCheckerActivity?
}
