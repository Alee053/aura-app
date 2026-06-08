package com.programovil.aura.pomodoro.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.pomodoro.domain.PomodoroDefaults
import com.programovil.aura.shared.FeatureFlag
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    LaunchedEffect(featureFlags) {
        if (featureFlags[FeatureFlag.POMODORO_ENABLED] == false) {
            onFeatureDisabled()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.pomodoro_content_description_settings),
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
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
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                val circleBackgroundColor = AppTheme.colors.textPrimary.copy(alpha = 0.1f)
                val progressColor = AppTheme.colors.textPrimary

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = circleBackgroundColor,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * uiState.progress,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (uiState.mode) {
                                PomodoroMode.POMODORO -> stringResource(Res.string.pomodoro_status_pomodoro)
                                PomodoroMode.SHORT_BREAK -> stringResource(Res.string.pomodoro_status_short_break)
                                PomodoroMode.LONG_BREAK -> stringResource(Res.string.pomodoro_status_long_break)
                            },
                            style = AppTheme.typography.labelMedium,
                            color = AppTheme.colors.textSecondary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatTime(uiState.timeLeftSeconds),
                            style = AppTheme.typography.displayLarge.copy(
                                fontSize = 64.sp,
                                fontWeight = FontWeight.ExtraLight
                            ),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PomodoroDefaults.MANUAL_DURATIONS_MINUTES.forEachIndexed { index, minutes ->
                        if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                        val labelRes = when (minutes) {
                            5 -> Res.string.pomodoro_5m
                            10 -> Res.string.pomodoro_10m
                            25 -> Res.string.pomodoro_25m
                            else -> Res.string.pomodoro_25m
                        }
                        TimeOption(
                            label = stringResource(labelRes),
                            isSelected = uiState.selectedOption == minutes,
                            onClick = { viewModel.onTimeOptionSelected(minutes) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(AppTheme.colors.textPrimary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.pomodoro_content_description_reset),
                            tint = AppTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    val playPauseBackgroundColor = AppTheme.colors.textPrimary.copy(alpha = 0.15f)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(84.dp)
                            .background(playPauseBackgroundColor, CircleShape)
                            .clickable { viewModel.toggleTimer() }
                    ) {
                        Icon(
                            imageVector = if (uiState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isRunning)
                                stringResource(Res.string.pomodoro_content_description_pause)
                            else
                                stringResource(Res.string.pomodoro_content_description_play),
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = { viewModel.skipSession() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(AppTheme.colors.textPrimary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = stringResource(Res.string.pomodoro_content_description_skip),
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    val sessionsCompletedText = stringResource(Res.string.pomodoro_sessions_completed, uiState.sessionsCompleted)
                    val nextSessionLabel = when (uiState.mode) {
                        PomodoroMode.POMODORO -> {
                            val sessionsAfterNext = uiState.sessionsCompleted + 1
                            if (sessionsAfterNext % PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK == 0)
                                stringResource(Res.string.pomodoro_status_long_break)
                            else
                                stringResource(Res.string.pomodoro_status_short_break)
                        }
                        PomodoroMode.SHORT_BREAK,
                        PomodoroMode.LONG_BREAK ->
                            stringResource(Res.string.pomodoro_status_pomodoro)
                    }
                    val nextSessionText = stringResource(Res.string.pomodoro_next_session, nextSessionLabel)

                    Text(
                        text = "$sessionsCompletedText | $nextSessionText",
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TimeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) AppTheme.colors.textPrimary.copy(alpha = 0.2f) else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, AppTheme.colors.textPrimary.copy(alpha = 0.1f)),
        modifier = Modifier.height(36.dp).width(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = AppTheme.typography.labelMedium,
                color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
}
