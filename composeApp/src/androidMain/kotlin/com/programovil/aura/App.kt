package com.programovil.aura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.navigation.AppNavHost
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    onSignInClick: () -> Unit = {}
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val authViewModel: AuthViewModel = koinInject()
            val authState by authViewModel.authState.collectAsState()

            var showSettings by remember { mutableStateOf(false) }
            var showTimePicker by remember { mutableStateOf(false) }

            val notificationViewModel: NotificationViewModel = koinInject()
            val isEnabled by notificationViewModel.isEnabled.collectAsState()
            val hour by notificationViewModel.hour.collectAsState()
            val minute by notificationViewModel.minute.collectAsState()

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = hour,
                    initialMinute = minute
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            notificationViewModel.setNotificationTime(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    }
                )
            }

            when (val state = authState) {
                is AuthViewModel.AuthState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AuthViewModel.AuthState.SignedIn -> {
                    if (showSettings) {
                        NotificationSettingsDialog(
                            isEnabled = isEnabled,
                            hour = hour,
                            minute = minute,
                            onEnabledChange = { notificationViewModel.setEnabled(it) },
                            onTimeClick = { showTimePicker = true },
                            onDismiss = { showSettings = false }
                        )
                    } else {
                        AppNavHost(
                            onNavigateToSettings = { showSettings = true }
                        )
                    }
                }
                is AuthViewModel.AuthState.SignedOut,
                is AuthViewModel.AuthState.Error -> {
                    SignInScreen(
                        errorMessage = if (state is AuthViewModel.AuthState.Error) state.message else null,
                        onSignInClick = onSignInClick
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsDialog(
    isEnabled: Boolean,
    hour: Int,
    minute: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("Notification Settings") },
        text = {
            Column {
                Text(
                    text = "Daily Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Status: ${if (isEnabled) "Enabled" else "Disabled"}")
                Text("Time: ${String.format("%02d:%02d", hour, minute)}")

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onTimeClick) {
                    Text("Change Time")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { onEnabledChange(!isEnabled) }) {
                    Text(if (isEnabled) "Disable Notifications" else "Enable Notifications")
                }
            }
        }
    )
}

@Composable
private fun SignInScreen(
    errorMessage: String?,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Aura",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sign in to sync your todos",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onSignInClick) {
            Text("Sign in with Google")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}