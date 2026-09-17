package com.programovil.aura.home.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.button.*
import com.programovil.aura.designsystem.components.state.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.home.presentation.viewmodel.*
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(viewModel: HomeViewModel, showTodos: Boolean = true, showHabits: Boolean = true,
    onTodoClick: () -> Unit = {}, onHabitClick: () -> Unit = {}, onSettingsClick: () -> Unit = {},
    showPomodoro: Boolean = true, showJournal: Boolean = true,
    onFocusClick: () -> Unit = {}, onJournalClick: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    HomeContent(state, showTodos, showHabits, showPomodoro, showJournal,
        onTodoClick, onHabitClick, onSettingsClick, onFocusClick, onJournalClick, viewModel::retryLoad)
}

@Composable
fun HomeContent(state: HomeUiState, showTodos: Boolean, showHabits: Boolean,
    showPomodoro: Boolean, showJournal: Boolean, onTodoClick: () -> Unit,
    onHabitClick: () -> Unit, onSettingsClick: () -> Unit, onFocusClick: () -> Unit,
    onJournalClick: () -> Unit, onRetry: () -> Unit) {
    val c = AppTheme.colors
    val motion = LocalAuraMotion.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = LocalAuraGutter.current).padding(bottom = AuraSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
        Row(Modifier.fillMaxWidth().padding(top = AuraSpacing.lg),
            verticalAlignment = Alignment.CenterVertically) {
            AuraBrandMark(Modifier.size(32.dp))
            Text(stringResource(Res.string.app_name_label), Modifier.weight(1f).padding(start = AuraSpacing.sm),
                style = AppTheme.typography.titleMedium)
            IconButton(onSettingsClick) { Icon(Icons.Outlined.Settings, stringResource(Res.string.settings_content_description)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
            Text(auraDate(todayDate()), style = AppTheme.typography.bodyMedium, color = c.textSecondary)
            Text(stringResource(Res.string.rd_home_title), style = AppTheme.typography.headlineLarge)
        }
        if (state.loadFailed) AuraInlineNotice(stringResource(Res.string.rd_summary_error),
            actionLabel = stringResource(Res.string.rd_retry), onAction = onRetry)
        if (state.isLoading && (showTodos || showHabits)) AuraSkeleton(rows = 2, label = stringResource(Res.string.rd_loading))
        else {
            if (showTodos) {
                Surface(color = c.hero, contentColor = c.onHero, shape = AuraShapes.card,
                    border = if (c.highContrast) BorderStroke(1.dp, c.outline) else null) {
                    Column(Modifier.fillMaxWidth().padding(AuraSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(Res.string.rd_pending_tasks), style = AppTheme.typography.bodyLarge)
                                AnimatedContent(if (state.hasData) state.dashboardData.incompleteTodos.toString() else "—",
                                    transitionSpec = { fadeIn(tween(motion.duration(220))) togetherWith fadeOut(tween(motion.interaction)) }, label = "pending") {
                                    Text(it, style = AppTheme.typography.displayLarge, modifier = Modifier.padding(top = AuraSpacing.xs))
                                }
                            }
                            AuraBrandMark(Modifier.size(72.dp), c.accent)
                        }
                        Text(stringResource(Res.string.rd_tasks_hint), style = AppTheme.typography.bodyMedium)
                        FilledTonalButton(onTodoClick, shape = AuraShapes.button,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.onHero, contentColor = c.hero)) {
                            Text(stringResource(Res.string.rd_view_tasks))
                            Spacer(Modifier.width(AuraSpacing.xs))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (showHabits) Surface(shape = AuraShapes.card, color = c.surface,
                border = if (c.highContrast) BorderStroke(1.dp, c.outline) else null) {
                Column(Modifier.padding(AuraSpacing.card), verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val data = state.dashboardData
                        val ratio = if (data.totalHabitsToday > 0) data.completedHabitsToday.toFloat() / data.totalHabitsToday else 0f
                        CircularProgressIndicator(progress = { if (state.hasData) ratio else 0f },
                            modifier = Modifier.size(56.dp), color = c.primary, trackColor = c.surfaceVariant, strokeWidth = 5.dp)
                        Column(Modifier.weight(1f).padding(start = AuraSpacing.md)) {
                            Text(stringResource(Res.string.rd_marked_today), color = c.textSecondary, style = AppTheme.typography.bodyMedium)
                            Text(if (state.hasData) "${data.completedHabitsToday} / ${data.totalHabitsToday}" else "—",
                                style = AppTheme.typography.headlineSmall)
                        }
                        IconButton(onHabitClick) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(Res.string.rd_view_habits)) }
                    }
                    if (state.hasData) Text(stringResource(Res.string.rd_streak, state.dashboardData.currentStreak),
                        style = AppTheme.typography.bodyMedium, color = c.textSecondary)
                }
            }
        }
        if (showPomodoro) Surface(onClick = onFocusClick, shape = AuraShapes.card, color = c.primaryContainer) {
            Row(Modifier.fillMaxWidth().padding(AuraSpacing.card), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, tint = c.primary)
                Column(Modifier.weight(1f).padding(horizontal = AuraSpacing.md)) {
                    Text(stringResource(Res.string.rd_open_focus), style = AppTheme.typography.titleMedium, color = c.onPrimaryContainer)
                    Text(stringResource(Res.string.rd_focus_hint), style = AppTheme.typography.bodyMedium, color = c.onPrimaryContainer)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = c.onPrimaryContainer)
            }
        } else if (showJournal) AuraButton(stringResource(Res.string.rd_open_journal), onJournalClick,
            Modifier.fillMaxWidth(), style = AuraButtonStyle.Tonal)
        if (!showTodos && !showHabits && !showPomodoro && !showJournal)
            Text(stringResource(Res.string.rd_unavailable), color = c.textSecondary)
        if (state.homeVariant.showsDailyMotivation && state.resolvedMotivationPhrase.isNotBlank()) {
            Column(Modifier.padding(vertical = AuraSpacing.sm), verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                Text(stringResource(Res.string.rd_motivation), color = c.primary, style = AppTheme.typography.labelLarge)
                Text(state.resolvedMotivationPhrase, style = AppTheme.typography.headlineSmall)
            }
        }
    }
}
