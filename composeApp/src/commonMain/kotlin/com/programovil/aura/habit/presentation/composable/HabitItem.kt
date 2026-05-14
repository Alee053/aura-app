package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.shared.parseHexColor
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.streak_format
import org.jetbrains.compose.resources.stringResource

@Composable
fun HabitItem(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStatus.habit
    val isDone = habitWithStatus.isDone
    val isMissed = habitWithStatus.isMissed

    Row(
        modifier = modifier
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(parseHexColor(habit.color))
        )

        // Name
        Text(
            text = habit.name,
            style = AppTheme.typography.bodyMedium.copy(
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                color = when {
                    isDone -> AppTheme.colors.textPrimary.copy(alpha = 0.6f)
                    isMissed -> AppTheme.colors.primary.copy(alpha = 0.7f)
                    else -> AppTheme.colors.textPrimary
                }
            ),
            modifier = Modifier.weight(1f)
        )

        // Streak badge
        if (habitWithStatus.streak > 0) {
            Text(
                text = stringResource(Res.string.streak_format, habitWithStatus.streak),
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.primary
            )
        }

        // Checkbox
        Checkbox(
            checked = isDone,
            onCheckedChange = { onToggle() }
        )
    }
}
