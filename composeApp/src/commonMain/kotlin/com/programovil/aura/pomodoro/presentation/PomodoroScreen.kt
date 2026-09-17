package com.programovil.aura.pomodoro.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.button.AuraButton
import com.programovil.aura.designsystem.components.header.AuraScreenHeader
import com.programovil.aura.designsystem.components.overlay.AuraConfirmDialog
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.pomodoro.domain.*
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel, featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    PomodoroContent(state, viewModel::toggleTimer, viewModel::resetTimer, viewModel::skipSession, viewModel::onTimeOptionSelected)
}

@Composable
fun PomodoroContent(state: PomodoroUiState, onToggle: () -> Unit, onReset: () -> Unit,
    onSkip: () -> Unit, onDuration: (Int) -> Unit) {
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    val hasProgress = state.isRunning || state.timeLeftSeconds < state.initialTimeSeconds
    fun request(action: String) {
        if (hasProgress) pendingAction = action else when(action) {
            "reset" -> onReset(); "skip" -> onSkip(); else -> onDuration(action.toInt())
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = maxWidth >= 600.dp && maxHeight < 600.dp
        val compact = maxHeight < 600.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = LocalAuraGutter.current).padding(bottom = AuraSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally) {
            AuraScreenHeader(stringResource(Res.string.rd_focus), stringResource(Res.string.rd_focus_hint))
            if (horizontal) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xl)) {
                FocusDial(state, true, Modifier.weight(1f))
                Column(Modifier.weight(1f)) { FocusControls(state, onToggle, ::request) }
            } else {
                FocusDial(state, compact, Modifier.fillMaxWidth())
                Spacer(Modifier.height(AuraSpacing.xl))
                FocusControls(state, onToggle, ::request)
            }
        }
    }
    pendingAction?.let { action ->
        AuraConfirmDialog(stringResource(Res.string.rd_timer_change), stringResource(Res.string.rd_timer_change_body),
            stringResource(Res.string.rd_continue), stringResource(Res.string.cancel),
            { pendingAction = null; when(action) { "reset" -> onReset(); "skip" -> onSkip(); else -> onDuration(action.toInt()) } },
            { pendingAction = null })
    }
}

@Composable
private fun FocusDial(state: PomodoroUiState, compact: Boolean, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val largeType = LocalDensity.current.fontScale >= 1.5f
    val diameter = if (compact) 208.dp else 264.dp
    var lastProgress by remember { mutableFloatStateOf(state.progress) }
    val motion = LocalAuraMotion.current
    val progress by animateFloatAsState(state.progress,
        animationSpec = if (!motion.enabled || abs(lastProgress - state.progress) > .02f) snap() else tween(200),
        label = "remaining arc")
    SideEffect { lastProgress = state.progress }
    val mode = pomodoroModeLabel(state.mode)
    val time = formatTime(state.timeLeftSeconds)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
        Surface(shape = AuraShapes.button, color = c.primaryContainer) {
            Text(mode, Modifier.padding(horizontal = AuraSpacing.md, vertical = AuraSpacing.xs),
                style = AppTheme.typography.labelLarge, color = c.onPrimaryContainer)
        }
        Box(Modifier.size(diameter).semantics {
            contentDescription = "$mode, $time"
            progressBarRangeInfo = ProgressBarRangeInfo(state.progress, 0f..1f)
        }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2 + 8.dp.toPx()
                if (!c.highContrast) drawCircle(Brush.radialGradient(listOf(c.primaryContainer.copy(alpha = .6f), c.background)),
                    radius = size.minDimension / 2)
                drawArc(c.surfaceVariant, -90f, 360f, false, Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(stroke))
                drawArc(c.primary, -90f, 360f * progress.coerceIn(0f,1f), false, Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            if (largeType) AuraBrandMark(Modifier.size(64.dp))
            else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics {}) {
                Text(time, style = AppTheme.typography.timer.copy(
                    fontSize = if (compact) 48.sp else 64.sp, lineHeight = if (compact) 56.sp else 72.sp))
                Text(stringResource(if (state.isRunning) Res.string.rd_running
                    else if (state.timeLeftSeconds < state.initialTimeSeconds) Res.string.rd_paused else Res.string.rd_ready),
                    style = AppTheme.typography.labelMedium, color = c.textSecondary)
            }
        }
        if (largeType) Text(time, style = AppTheme.typography.timer.copy(fontSize = 48.sp))
    }
}

@Composable
private fun FocusControls(state: PomodoroUiState, onToggle: () -> Unit, request: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
            PomodoroDefaults.MANUAL_DURATIONS_MINUTES.sorted().forEach { minutes ->
                FilterChip(state.selectedOption == minutes, { request(minutes.toString()) },
                    label = { Text(stringResource(when(minutes) { 5 -> Res.string.pomodoro_5m; 10 -> Res.string.pomodoro_10m; else -> Res.string.pomodoro_25m })) },
                    modifier = Modifier.heightIn(min = AuraSpacing.touch))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                IconButton({ request("reset") }) { Icon(Icons.Outlined.Refresh, stringResource(Res.string.rd_reset)) }
                Text(stringResource(Res.string.rd_reset), style = AppTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.3f)) {
                FilledIconButton(onToggle, Modifier.size(80.dp), shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AppTheme.colors.primary, contentColor = AppTheme.colors.onPrimary)) {
                    AnimatedContent(state.isRunning, label = "play pause") { running ->
                        Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            stringResource(if (running) Res.string.rd_pause else Res.string.rd_start), Modifier.size(36.dp))
                    }
                }
                Text(stringResource(if (state.isRunning) Res.string.rd_pause else Res.string.rd_start),
                    style = AppTheme.typography.labelLarge, modifier = Modifier.padding(top = AuraSpacing.xs))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                IconButton({ request("skip") }) { Icon(Icons.Outlined.SkipNext, stringResource(Res.string.rd_skip)) }
                Text(stringResource(Res.string.rd_skip), style = AppTheme.typography.labelMedium)
            }
        }
        HorizontalDivider(color = AppTheme.colors.outline)
        Text(stringResource(Res.string.rd_next, pomodoroNextSessionLabel(state.mode, state.sessionsCompleted)),
            style = AppTheme.typography.bodyLarge, color = AppTheme.colors.textPrimary)
        Text(stringResource(Res.string.rd_sessions, state.sessionsCompleted),
            style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
    }
}

@Composable
fun PomodoroCompletionOverlay(completedMode: PomodoroMode?, nextSessionLabel: String, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, containerColor = AppTheme.colors.surface,
        icon = { AuraBrandMark(Modifier.size(64.dp)) },
        title = { Text(stringResource(Res.string.rd_finish_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
            Text(stringResource(if (completedMode == PomodoroMode.POMODORO || completedMode == null)
                Res.string.pomodoro_completion_pomodoro_message else Res.string.pomodoro_completion_break_message))
            Text(stringResource(Res.string.rd_next, nextSessionLabel), color = AppTheme.colors.primary)
        } },
        confirmButton = { AuraButton(stringResource(Res.string.rd_continue), onClose) })
}

@Composable fun pomodoroModeLabel(mode: PomodoroMode) = stringResource(when(mode) {
    PomodoroMode.POMODORO -> Res.string.pomodoro_status_pomodoro
    PomodoroMode.SHORT_BREAK -> Res.string.pomodoro_status_short_break
    PomodoroMode.LONG_BREAK -> Res.string.pomodoro_status_long_break
})
@Composable fun pomodoroNextSessionLabel(mode: PomodoroMode, sessionsCompleted: Int) = pomodoroModeLabel(
    if (mode == PomodoroMode.POMODORO) {
        if ((sessionsCompleted + 1) % PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK == 0) PomodoroMode.LONG_BREAK else PomodoroMode.SHORT_BREAK
    } else PomodoroMode.POMODORO
)
private fun formatTime(seconds: Int) = "${(seconds / 60).toString().padStart(2,'0')}:${(seconds % 60).toString().padStart(2,'0')}"
