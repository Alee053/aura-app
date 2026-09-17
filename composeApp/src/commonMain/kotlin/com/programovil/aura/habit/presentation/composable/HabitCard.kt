package com.programovil.aura.habit.presentation.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.habit.domain.model.*
import com.programovil.aura.shared.parseHexColor
import com.programovil.aura.shared.presentation.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDate

@Composable
fun HabitCard(habitWithStatus: HabitWithStatus, onToggle: (String) -> Unit, onLongClick: () -> Unit,
    modifier: Modifier = Modifier, pendingDates: Set<String> = emptySet()) {
    val item = habitWithStatus
    val habit = item.habit
    val c = AppTheme.colors
    val today = todayDate().toString()
    val progress by animateFloatAsState((item.currentPeriodProgress.first.toFloat() /
        item.currentPeriodProgress.second.coerceAtLeast(1)).coerceIn(0f,1f),
        tween(LocalAuraMotion.current.duration(240)), label = "habit progress")
    Surface(modifier.fillMaxWidth(), shape = AuraShapes.card, color = c.surface,
        border = if (c.highContrast) BorderStroke(1.dp, c.outline) else null) {
        Column(Modifier.padding(AuraSpacing.card), verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(parseHexColor(habit.color)))
                Column(Modifier.weight(1f).padding(start = AuraSpacing.sm)) {
                    Text(habit.name, style = AppTheme.typography.titleMedium)
                    Text(stringResource(when(habit.recurrenceType) {
                        RecurrenceType.DAILY -> Res.string.daily
                        RecurrenceType.WEEKLY -> Res.string.weekly
                        RecurrenceType.MONTHLY -> Res.string.monthly
                    }), style = AppTheme.typography.bodyMedium, color = c.textSecondary)
                }
                IconButton(onLongClick) { Icon(Icons.Outlined.Edit, stringResource(Res.string.rd_edit)) }
            }
            val doneToday = item.last7Days.any { it.date == today && it.isCompleted }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.rd_period_progress,
                        item.currentPeriodProgress.first, item.currentPeriodProgress.second,
                        stringResource(when(habit.recurrenceType) {
                            RecurrenceType.DAILY -> Res.string.habit_period_day
                            RecurrenceType.WEEKLY -> Res.string.habit_period_week
                            RecurrenceType.MONTHLY -> Res.string.habit_period_month
                        })), style = AppTheme.typography.labelLarge)
                    if (item.streak > 0) Text(stringResource(Res.string.rd_streak, item.streak),
                        style = AppTheme.typography.labelMedium, color = c.textSecondary)
                }
                TextButton({ onToggle(today) }, enabled = today !in pendingDates) {
                    Icon(if (doneToday) Icons.Outlined.CheckCircle else Icons.Outlined.AddCircleOutline, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(AuraSpacing.xxs))
                    Text(stringResource(if (doneToday) Res.string.rd_complete else Res.string.rd_mark_today))
                }
            }
            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth(),
                color = c.primary, trackColor = c.surfaceVariant)
            Text(stringResource(Res.string.rd_last_seven), style = AppTheme.typography.labelMedium, color = c.textSecondary)
            val scroll = rememberScrollState()
            LaunchedEffect(scroll.maxValue) { if (scroll.maxValue > 0) scroll.scrollTo(scroll.maxValue) }
            Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.SpaceBetween) {
                item.last7Days.forEach { day ->
                    val date = LocalDate.parse(day.date)
                    val fullLabel = "${auraDate(date)}, ${stringResource(if (day.isCompleted) Res.string.rd_complete else Res.string.rd_not_complete)}"
                    Column(Modifier.width(AuraSpacing.touch).toggleable(day.isCompleted,
                        enabled = day.date !in pendingDates, role = Role.Checkbox, onValueChange = { onToggle(day.date) })
                        .clearAndSetSemantics {
                            contentDescription = fullLabel
                            role = Role.Checkbox
                            toggleableState = if (day.isCompleted) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off
                            if (day.date in pendingDates) disabled() else onClick { onToggle(day.date); true }
                        }.padding(vertical = AuraSpacing.xxs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
                        Text(auraDay(date), style = AppTheme.typography.labelMedium, color = c.textSecondary)
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .background(if (day.isCompleted) c.primary else c.surfaceVariant)
                            .border(if (day.date == today) 2.dp else 0.dp,
                                if (day.date == today) c.textPrimary else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center) {
                            if (day.date in pendingDates) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else if (day.isCompleted) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = c.onPrimary)
                            else Text(date.dayOfMonth.toString(), style = AppTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
