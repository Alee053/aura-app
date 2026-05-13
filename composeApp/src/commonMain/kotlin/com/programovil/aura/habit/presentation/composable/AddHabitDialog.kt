package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.color_label
import aura_app.composeapp.generated.resources.daily
import aura_app.composeapp.generated.resources.habit_name
import aura_app.composeapp.generated.resources.new_habit
import aura_app.composeapp.generated.resources.repeat_on
import aura_app.composeapp.generated.resources.save
import aura_app.composeapp.generated.resources.weekly
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import org.jetbrains.compose.resources.stringResource

private val colorPalette = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD"
)

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, recurrenceType: RecurrenceType, daysOfWeek: List<Int>, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableIntStateOf(0) }
    var selectedColor by remember { mutableStateOf(colorPalette[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.new_habit),
                    style = AppTheme.typography.headlineSmall
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.daily),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                    Switch(
                        checked = !isDaily,
                        onCheckedChange = { isDaily = !it }
                    )
                    Text(
                        stringResource(Res.string.weekly),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                }

                if (!isDaily) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.repeat_on),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val dayBit = 1 shl index
                            val isSelected = (selectedDays and dayBit) != 0
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AppTheme.colors.primary
                                        else AppTheme.colors.surface.copy(alpha = 0.6f)
                                    )
                                    .clickable {
                                        selectedDays = selectedDays xor dayBit
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = AppTheme.typography.labelLarge,
                                    color = if (isSelected) AppTheme.colors.textPrimary
                                    else AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.color_label),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    horizontalArrangement = Arrangement.End
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onDismiss,
                        enabled = true
                    )
                    Spacer(modifier = Modifier.weight(1f).size(0.dp))
                    PrimaryButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            if (name.isNotBlank()) {
                                val daysOfWeek = if (isDaily) {
                                    emptyList()
                                } else {
                                    (0..6).filter { (selectedDays and (1 shl it)) != 0 }.map { it + 1 }
                                }
                                onSave(name, if (isDaily) RecurrenceType.DAILY else RecurrenceType.WEEKLY, daysOfWeek, selectedColor)
                            }
                        },
                        enabled = name.isNotBlank()
                    )
                }
            }
        }
    }
}
