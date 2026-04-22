package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): NotificationPermissionState {
    return remember {
        object : NotificationPermissionState {
            override val hasPermission: Boolean = true
            override val shouldShowRationale: Boolean = false
            override fun launchPermissionRequest() {
                onPermissionResult(true)
            }
        }
    }
}
