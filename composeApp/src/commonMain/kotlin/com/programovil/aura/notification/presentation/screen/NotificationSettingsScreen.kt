package com.programovil.aura.notification.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import com.programovil.aura.shared.presentation.rememberNotificationPermissionState
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.notification_settings
import aura_app.composeapp.generated.resources.back
import aura_app.composeapp.generated.resources.daily_summary
import aura_app.composeapp.generated.resources.enabled
import aura_app.composeapp.generated.resources.test_notification
import aura_app.composeapp.generated.resources.notification_time
import aura_app.composeapp.generated.resources.notification_helper
import aura_app.composeapp.generated.resources.notification_permission
import aura_app.composeapp.generated.resources.notification_permission_text
import aura_app.composeapp.generated.resources.ok
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.allow
import aura_app.composeapp.generated.resources.deny
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationViewModel,
    onBackClick: () -> Unit
) {
    val isEnabled by viewModel.isEnabled.collectAsState()
    val hour by viewModel.hour.collectAsState()
    val minute by viewModel.minute.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionState = rememberNotificationPermissionState { isGranted ->
        if (isGranted) {
            viewModel.setEnabled(true)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNotificationTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.notification_settings)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.daily_summary),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.enabled),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !permissionState.hasPermission) {
                            showPermissionDialog = true
                        } else {
                            viewModel.setEnabled(enabled)
                        }
                    }
                )
            }

            Button(
                onClick = { viewModel.testNotification() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(stringResource(Res.string.test_notification))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.notification_time),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(Res.string.notification_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionState.launchPermissionRequest()
                }) {
                    Text(stringResource(Res.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(Res.string.deny))
                }
            },
            title = { Text(stringResource(Res.string.notification_permission)) },
            text = { Text(stringResource(Res.string.notification_permission_text)) }
        )
    }
}
