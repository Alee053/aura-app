package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.daily_badge
import aura_app.composeapp.generated.resources.streak_format
import org.jetbrains.compose.resources.stringResource

@Composable
fun HabitItem(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStatus.habit
    val isDone = habitWithStatus.isDone
    val isMissed = habitWithStatus.isMissed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Color dot + Name + Streak + Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(habit.color))
                )

                Text(
                    text = habit.name,
                    style = AppTheme.typography.bodyMedium,
                    color = when {
                        isDone -> AppTheme.colors.textPrimary.copy(alpha = 0.6f)
                        isMissed -> AppTheme.colors.primary.copy(alpha = 0.7f)
                        else -> AppTheme.colors.textPrimary
                    },
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )

                if (habitWithStatus.streak > 0) {
                    Text(
                        text = stringResource(Res.string.streak_format, habitWithStatus.streak),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.primary
                    )
                }

                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppTheme.colors.primary,
                        uncheckedColor = AppTheme.colors.textSecondary
                    )
                )
            }

            // Row 2: Recurrence badge
            RecurrenceBadge(habit = habit)

            // Row 3: Weekly heatmap strip
            WeeklyHeatmap(
                completions = habitWithStatus.weeklyCompletions,
                targetDate = habitWithStatus.targetDate
            )
        }
    }
}

@Composable
private fun RecurrenceBadge(habit: Habit) {
    val label = if (habit.recurrenceType == RecurrenceType.DAILY) {
        stringResource(Res.string.daily_badge)
    } else {
        habit.daysOfWeek.sorted().joinToString(" ") { day ->
            when (day) {
                1 -> "M"
                2 -> "T"
                3 -> "W"
                4 -> "T"
                5 -> "F"
                6 -> "S"
                7 -> "S"
                else -> ""
            }
        }
    }

    Text(
        text = label,
        style = AppTheme.typography.labelLarge,
        color = AppTheme.colors.textSecondary
    )
}

@Composable
private fun WeeklyHeatmap(
    completions: List<DayCompletion>,
    targetDate: String
) {
    if (completions.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        completions.forEachIndexed { index, day ->
            val circleColor = if (day.isCompleted) AppTheme.colors.primary
                              else AppTheme.colors.background
            val borderModifier = if (day.isScheduled && !day.isCompleted) {
                Modifier.border(1.dp, AppTheme.colors.textSecondary, CircleShape)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .then(borderModifier)
            )
        }
    }
}