package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.color_label
import aura_app.composeapp.generated.resources.daily
import aura_app.composeapp.generated.resources.delete_habit
import aura_app.composeapp.generated.resources.edit_habit
import aura_app.composeapp.generated.resources.habit_name
import aura_app.composeapp.generated.resources.monthly
import aura_app.composeapp.generated.resources.new_habit
import aura_app.composeapp.generated.resources.per_month
import aura_app.composeapp.generated.resources.per_week
import aura_app.composeapp.generated.resources.save
import aura_app.composeapp.generated.resources.weekly
import org.jetbrains.compose.resources.stringResource

private val colorPalette = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD"
)

@Composable
private fun RecurrenceSelector(
    selected: RecurrenceType,
    onSelect: (RecurrenceType) -> Unit
) {
    val options = listOf(
        RecurrenceType.DAILY to stringResource(Res.string.daily),
        RecurrenceType.WEEKLY to stringResource(Res.string.weekly),
        RecurrenceType.MONTHLY to stringResource(Res.string.monthly)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (type, label) ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) AppTheme.colors.primary
                        else AppTheme.colors.surface.copy(alpha = 0.6f)
                    )
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = AppTheme.typography.labelLarge,
                    color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun TargetCountStepper(
    count: Int,
    max: Int,
    onCountChange: (Int) -> Unit
) {
    val label = if (max == 7) stringResource(Res.string.per_week) else stringResource(Res.string.per_month)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = { if (count > 1) onCountChange(count - 1) },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surface.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = AppTheme.colors.textPrimary
            )
        }
        Text(
            text = "$count $label",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textPrimary
        )
        IconButton(
            onClick = { if (count < max) onCountChange(count + 1) },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surface.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = AppTheme.colors.textPrimary
            )
        }
    }
}

@Composable
fun HabitDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (habit: Habit) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var recurrenceType by remember {
        mutableStateOf(habit?.recurrenceType ?: RecurrenceType.DAILY)
    }
    var targetCount by remember {
        mutableIntStateOf(habit?.targetCount ?: 1)
    }
    var selectedColor by remember { mutableStateOf(habit?.color ?: colorPalette[0]) }

    val isEditMode = habit != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(if (isEditMode) Res.string.edit_habit else Res.string.new_habit),
                    style = AppTheme.typography.headlineSmall,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                BasicInput(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.habit_name),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                RecurrenceSelector(
                    selected = recurrenceType,
                    onSelect = { recurrenceType = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (recurrenceType != RecurrenceType.DAILY) {
                    TargetCountStepper(
                        count = targetCount,
                        max = if (recurrenceType == RecurrenceType.WEEKLY) 7 else 31,
                        onCountChange = { targetCount = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.color_label),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorPalette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.border(2.dp, AppTheme.colors.textPrimary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditMode && onDelete != null) {
                        TextButton(onClick = {
                            onDelete()
                            onDismiss()
                        }) {
                            Text(
                                text = stringResource(Res.string.delete_habit),
                                style = AppTheme.typography.labelLarge,
                                color = AppTheme.colors.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onDismiss
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    PrimaryButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            if (name.isNotBlank()) {
                                val habitToSave = Habit(
                                    id = habit?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    recurrenceType = recurrenceType,
                                    targetCount = if (recurrenceType == RecurrenceType.DAILY) 1 else targetCount,
                                    color = selectedColor,
                                    createdAt = habit?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(habitToSave)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank()
                    )
                }
            }
        }
    }
}