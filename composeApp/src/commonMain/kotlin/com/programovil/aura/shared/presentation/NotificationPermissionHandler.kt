package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): NotificationPermissionState

interface NotificationPermissionState {
    val hasPermission: Boolean
    val shouldShowRationale: Boolean
    fun launchPermissionRequest()
}
