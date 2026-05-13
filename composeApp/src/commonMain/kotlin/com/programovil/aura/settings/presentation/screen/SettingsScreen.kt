package com.programovil.aura.settings.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.settings.presentation.composable.PreferenceItem
import com.programovil.aura.settings.presentation.composable.ThemeCard
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.presentation.rememberNotificationPermissionState

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionState = rememberNotificationPermissionState(
        onPermissionResult = { granted ->
            if (granted) {
                viewModel.setNotificationsEnabled(true)
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.background,
                        AppTheme.colors.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(48.dp))
                Text(
                    text = "Settings",
                    style = AppTheme.typography.headlineLarge,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Logout",
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Themes",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ThemeCard(
                name = "Arctic Night",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A237E), androidx.compose.ui.graphics.Color(0xFF6C5CE7)),
                isSelected = currentThemeMode == ThemeMode.PURPLE,
                onSelect = { onThemeChange(ThemeMode.PURPLE) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Forest Dawn",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF0A2B23), androidx.compose.ui.graphics.Color(0xFF16A085)),
                isSelected = currentThemeMode == ThemeMode.GREEN,
                onSelect = { onThemeChange(ThemeMode.GREEN) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Silent Desert",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF2C0B0B), androidx.compose.ui.graphics.Color(0xFFB33939)),
                isSelected = currentThemeMode == ThemeMode.RED,
                onSelect = { onThemeChange(ThemeMode.RED) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "Midnight",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF121212), androidx.compose.ui.graphics.Color(0xFFBB86EC)),
                isSelected = currentThemeMode == ThemeMode.DARK,
                onSelect = { onThemeChange(ThemeMode.DARK) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = "High Contrast",
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF000000), androidx.compose.ui.graphics.Color(0xFFFFFF00)),
                isSelected = currentThemeMode == ThemeMode.HIGH_CONTRAST,
                onSelect = { onThemeChange(ThemeMode.HIGH_CONTRAST) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Preferences",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PreferenceItem(
                title = "Notifications",
                subtitle = "Receive daily task reminders",
                checked = uiState.notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (permissionState.hasPermission) {
                            viewModel.setNotificationsEnabled(true)
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    } else {
                        viewModel.setNotificationsEnabled(false)
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AURA",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Version 1.0.0",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Made with love for mindful productivity",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
