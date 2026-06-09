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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.DarkPalette
import com.programovil.aura.designsystem.theme.GreenPalette
import com.programovil.aura.designsystem.theme.HighContrastPalette
import com.programovil.aura.designsystem.theme.PurplePalette
import com.programovil.aura.designsystem.theme.RedPalette
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.settings.presentation.composable.PreferenceItem
import com.programovil.aura.settings.presentation.composable.ThemeCard
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.presentation.rememberNotificationPermissionState
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.settings_title
import aura_app.composeapp.generated.resources.logout_button
import aura_app.composeapp.generated.resources.themes_section
import aura_app.composeapp.generated.resources.preferences_section
import aura_app.composeapp.generated.resources.purple_theme
import aura_app.composeapp.generated.resources.green_theme
import aura_app.composeapp.generated.resources.red_theme
import aura_app.composeapp.generated.resources.dark_theme
import aura_app.composeapp.generated.resources.high_contrast_theme
import aura_app.composeapp.generated.resources.reminder_time_label
import aura_app.composeapp.generated.resources.app_name_label
import aura_app.composeapp.generated.resources.version
import aura_app.composeapp.generated.resources.made_with_love
import org.jetbrains.compose.resources.stringResource

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
                    text = stringResource(Res.string.settings_title),
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
                        text = stringResource(Res.string.logout_button),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.themes_section),
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ThemeCard(
                name = stringResource(Res.string.purple_theme),
                colors = listOf(PurplePalette.background, PurplePalette.primary),
                isSelected = currentThemeMode == ThemeMode.PURPLE,
                onSelect = { onThemeChange(ThemeMode.PURPLE) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = stringResource(Res.string.green_theme),
                colors = listOf(GreenPalette.background, GreenPalette.primary),
                isSelected = currentThemeMode == ThemeMode.GREEN,
                onSelect = { onThemeChange(ThemeMode.GREEN) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = stringResource(Res.string.red_theme),
                colors = listOf(RedPalette.background, RedPalette.primary),
                isSelected = currentThemeMode == ThemeMode.RED,
                onSelect = { onThemeChange(ThemeMode.RED) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = stringResource(Res.string.dark_theme),
                colors = listOf(DarkPalette.background, DarkPalette.primary),
                isSelected = currentThemeMode == ThemeMode.DARK,
                onSelect = { onThemeChange(ThemeMode.DARK) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeCard(
                name = stringResource(Res.string.high_contrast_theme),
                colors = listOf(HighContrastPalette.background, HighContrastPalette.primary),
                isSelected = currentThemeMode == ThemeMode.HIGH_CONTRAST,
                onSelect = { onThemeChange(ThemeMode.HIGH_CONTRAST) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(Res.string.preferences_section),
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

            if (uiState.notificationsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.reminder_time_label),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    var hourText by remember { mutableStateOf(uiState.notificationHour.toString()) }
                    var minuteText by remember { mutableStateOf(uiState.notificationMinute.toString()) }

                    LaunchedEffect(uiState.notificationHour) {
                        hourText = uiState.notificationHour.toString()
                    }
                    LaunchedEffect(uiState.notificationMinute) {
                        minuteText = uiState.notificationMinute.toString()
                    }

                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.filter { it.isDigit() }
                            if (digitsOnly.length <= 2) {
                                hourText = digitsOnly
                                digitsOnly.toIntOrNull()?.let { hour ->
                                    viewModel.setNotificationTime(hour.coerceIn(0, 23), uiState.notificationMinute)
                                }
                            }
                        },
                        modifier = Modifier.width(64.dp),
                        singleLine = true,
                        textStyle = AppTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary,
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
                            focusedLabelColor = AppTheme.colors.primary,
                            unfocusedLabelColor = AppTheme.colors.textSecondary,
                            cursorColor = AppTheme.colors.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(
                        text = " : ",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.filter { it.isDigit() }
                            if (digitsOnly.length <= 2) {
                                minuteText = digitsOnly
                                digitsOnly.toIntOrNull()?.let { minute ->
                                    viewModel.setNotificationTime(uiState.notificationHour, minute.coerceIn(0, 59))
                                }
                            }
                        },
                        modifier = Modifier.width(64.dp),
                        singleLine = true,
                        textStyle = AppTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary,
                            focusedBorderColor = AppTheme.colors.primary,
                            unfocusedBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
                            focusedLabelColor = AppTheme.colors.primary,
                            unfocusedLabelColor = AppTheme.colors.textSecondary,
                            cursorColor = AppTheme.colors.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(Res.string.app_name_label),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(Res.string.version),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.made_with_love),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
