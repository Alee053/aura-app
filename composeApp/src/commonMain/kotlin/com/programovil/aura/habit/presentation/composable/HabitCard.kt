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
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HabitCard(
    habitWithStatus: HabitWithStatus,
    onToggle: (date: String) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStatus.habit
    val (completed, target) = habitWithStatus.currentPeriodProgress
    val streak = habitWithStatus.streak
    val last7Days = habitWithStatus.last7Days
    val habitColor = parseHexColor(habit.color)

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(habitColor)
                )

                Text(
                    text = habit.name,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (habit.recurrenceType != RecurrenceType.DAILY) {
                    Text(
                        text = "$completed/$target",
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary
                    )
                }

                if (streak > 0) {
                    Text(
                        text = streak.toString(),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.primary
                    )
                }

                val todayCompleted = last7Days.lastOrNull()?.isCompleted == true
                Checkbox(
                    checked = todayCompleted,
                    onCheckedChange = { _ -> onToggle(today.toString()) },  // toggle is symmetric: check=add, uncheck=remove
                    colors = CheckboxDefaults.colors(
                        checkedColor = habitColor,
                        uncheckedColor = AppTheme.colors.textSecondary
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }

            SevenDayGrid(
                last7Days = last7Days,
                habitColor = habitColor,
                today = today,
                onToggle = onToggle
            )
        }
    }
}

@Composable
private fun SevenDayGrid(
    last7Days: List<DayCompletion>,
    habitColor: androidx.compose.ui.graphics.Color,
    today: kotlinx.datetime.LocalDate,
    onToggle: (date: String) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        last7Days.forEachIndexed { index, dayCompletion ->
            val isToday = dayCompletion.date == today.toString()
            DayCircle(
                dayCompletion = dayCompletion,
                dayLabel = dayLabels.getOrElse(index) { "" },
                habitColor = habitColor,
                isToday = isToday,
                onToggle = { onToggle(dayCompletion.date) }
            )
        }
    }
}

@Composable
private fun DayCircle(
    dayCompletion: DayCompletion,
    dayLabel: String,
    habitColor: androidx.compose.ui.graphics.Color,
    isToday: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (dayCompletion.isCompleted) habitColor
                    else AppTheme.colors.background
                )
                .then(
                    if (isToday) Modifier.border(2.dp, AppTheme.colors.textPrimary, CircleShape)
                    else Modifier.border(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.3f), CircleShape)
                )
                .clickable(onClick = onToggle)
        )
        Text(
            text = dayLabel,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary
        )
    }
}