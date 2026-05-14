package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberNotificationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): NotificationPermissionState {
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getNotificationSettingsWithCompletionHandler { settings ->
            settings?.let {
                hasPermission = it.authorizationStatus == UNAuthorizationStatusAuthorized
            }
        }
    }

    return remember(hasPermission) {
        object : NotificationPermissionState {
            override val hasPermission: Boolean = hasPermission
            override val shouldShowRationale: Boolean = false
            override fun launchPermissionRequest() {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, error ->
                    if (error != null) {
                        println("Notification permission error: $error")
                    }
                    hasPermission = granted
                    onPermissionResult(granted)
                }
            }
        }
    }
}
