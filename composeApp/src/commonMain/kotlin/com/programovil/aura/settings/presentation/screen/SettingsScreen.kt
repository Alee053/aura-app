package com.programovil.aura.settings.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.programovil.aura.designsystem.components.input.AuraTextField
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.overlay.*
import com.programovil.aura.designsystem.components.state.AuraInlineNotice
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.settings.presentation.composable.*
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit, onSignOut: () -> Unit, onNavigateBack: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    var logout by remember { mutableStateOf(false) }
    var editTime by rememberSaveable { mutableStateOf(false) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberNotificationPermissionState { granted ->
        denied = !granted
        if (granted) viewModel.setNotificationsEnabled(true)
    }
    val largeType = LocalDensity.current.fontScale >= 1.5f
    val is24Hour = uses24HourClock()
    val themes = listOf(ThemeMode.PURPLE, ThemeMode.GREEN, ThemeMode.RED, ThemeMode.DARK, ThemeMode.HIGH_CONTRAST)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = LocalAuraGutter.current)
        .padding(bottom = AuraSpacing.xl), verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
        Row(Modifier.padding(top = AuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onNavigateBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.journal_back)) }
            Text(stringResource(Res.string.settings_title), style = AppTheme.typography.headlineSmall)
        }
        Text(stringResource(Res.string.rd_appearance), style = AppTheme.typography.titleMedium)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
            themes.chunked(if (largeType) 1 else 2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                    row.forEach { theme ->
                        val palette = paletteFor(theme)
                        Box(Modifier.weight(1f)) {
                            ThemeCard(stringResource(when(theme) {
                                ThemeMode.PURPLE -> Res.string.rd_theme_aura
                                ThemeMode.GREEN -> Res.string.rd_theme_forest
                                ThemeMode.RED -> Res.string.rd_theme_clay
                                ThemeMode.DARK -> Res.string.rd_theme_midnight
                                ThemeMode.HIGH_CONTRAST -> Res.string.rd_theme_contrast
                            }), listOf(palette.background, palette.primary), theme == currentThemeMode, { onThemeChange(theme) })
                        }
                    }
                    if (!largeType && row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Text(stringResource(Res.string.notifications), style = AppTheme.typography.titleMedium)
        PreferenceItem(stringResource(Res.string.notifications), stringResource(Res.string.notifications_subtitle),
            checked = state.notificationsEnabled && permission.hasPermission,
            onCheckedChange = { enabled ->
                if (!enabled) viewModel.setNotificationsEnabled(false)
                else if (permission.hasPermission) viewModel.setNotificationsEnabled(true)
                else permission.launchPermissionRequest()
            })
        if (denied || (state.notificationsEnabled && !permission.hasPermission))
            AuraInlineNotice(stringResource(Res.string.rd_permission_denied), isError = false)
        AnimatedVisibility(state.notificationsEnabled && permission.hasPermission) {
            Surface(onClick = { editTime = true }, shape = AuraShapes.input, color = AppTheme.colors.surface) {
                Row(Modifier.fillMaxWidth().padding(AuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null)
                    Text(stringResource(Res.string.reminder_time_label), Modifier.weight(1f).padding(horizontal = AuraSpacing.md))
                    Text(auraClockTime(state.notificationHour, state.notificationMinute, is24Hour),
                        style = AppTheme.typography.labelLarge)
                }
            }
        }
        HorizontalDivider(color = AppTheme.colors.outline)
        Text(stringResource(Res.string.rd_session), style = AppTheme.typography.titleMedium)
        TextButton({ logout = true }) { Text(stringResource(Res.string.logout_button), color = AppTheme.colors.error) }
    }
    if (logout) AuraConfirmDialog(stringResource(Res.string.rd_logout_title), stringResource(Res.string.rd_logout_body),
        stringResource(Res.string.logout_button), stringResource(Res.string.cancel), { logout = false; onSignOut() }, { logout = false })
    if (editTime) {
        RegisterAuraOverlay()
        val time = rememberTimePickerState(state.notificationHour, state.notificationMinute, is24Hour)
        var hourText by rememberSaveable { mutableStateOf((if (is24Hour) state.notificationHour else (state.notificationHour % 12).let { if (it == 0) 12 else it }).toString()) }
        var minuteText by rememberSaveable { mutableStateOf(state.notificationMinute.toString().padStart(2, '0')) }
        var afternoon by rememberSaveable { mutableStateOf(state.notificationHour >= 12) }
        val inputHour = hourText.toIntOrNull()
        val inputMinute = minuteText.toIntOrNull()
        val valid = !largeType || (inputHour != null && inputHour in (if (is24Hour) 0..23 else 1..12) && inputMinute != null && inputMinute in 0..59)
        var inputMode by rememberSaveable { mutableStateOf(true) }
        AlertDialog(onDismissRequest = { editTime = false }, title = { Text(stringResource(Res.string.reminder_time_label)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (largeType) Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
                        AuraTextField(hourText, { hourText = it.filter(Char::isDigit).take(2) },
                            stringResource(Res.string.rd_hour), Modifier.fillMaxWidth(),
                            supportingText = if (is24Hour) "0–23" else "1–12", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        AuraTextField(minuteText, { minuteText = it.filter(Char::isDigit).take(2) },
                            stringResource(Res.string.rd_minute), Modifier.fillMaxWidth(),
                            supportingText = "0–59", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        if (!is24Hour) Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
                            FilterChip(!afternoon, { afternoon = false }, { Text(stringResource(Res.string.rd_am)) })
                            FilterChip(afternoon, { afternoon = true }, { Text(stringResource(Res.string.rd_pm)) })
                        }
                    } else if (inputMode) TimeInput(time) else TimePicker(time)
                    if (!largeType) TextButton({ inputMode = !inputMode }) {
                        Text(stringResource(if (inputMode) Res.string.rd_time_clock else Res.string.rd_time_input))
                    }
                }
            },
            confirmButton = { TextButton({
                val hour = if (!largeType) time.hour else if (is24Hour) inputHour!! else inputHour!! % 12 + if (afternoon) 12 else 0
                viewModel.setNotificationTime(hour, if (largeType) inputMinute!! else time.minute)
                editTime = false
            }, enabled = valid) { Text(stringResource(Res.string.rd_save)) } },
            dismissButton = { TextButton({ editTime = false }) { Text(stringResource(Res.string.cancel)) } })
    }
}
