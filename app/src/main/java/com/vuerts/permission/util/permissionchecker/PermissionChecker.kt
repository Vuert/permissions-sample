package com.vuerts.permission.util.permissionchecker

interface PermissionChecker {

    /**
     * Checks permissions
     *
     * @return [Result] either successful with unit type or with
     * [PermissionChecker.PermissionsDeniedException] that contains set of denied permissions
     */
    suspend fun checkPermissions(vararg permissions: String): Result<Unit>

    class PermissionsDeniedException(val permissions: Set<String>) : Exception()
}
