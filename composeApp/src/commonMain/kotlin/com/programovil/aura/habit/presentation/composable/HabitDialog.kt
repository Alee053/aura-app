package com.programovil.aura.habit.presentation.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.input.AuraTextField
import com.programovil.aura.designsystem.components.overlay.AuraEditorSheet
import com.programovil.aura.designsystem.components.selection.AuraSelectionRow
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.habit.domain.model.*
import com.programovil.aura.shared.parseHexColor
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.*

private val habitColors = listOf("#FF8D70", "#51B89E", "#6A9AE8", "#7C6AE6", "#E9B949", "#B779D1")

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Composable
fun HabitDialog(habit: Habit?, onDismiss: () -> Unit, onSave: (Habit) -> Unit,
    onDelete: (() -> Unit)? = null, operation: UiOperation? = null) {
    var name by rememberSaveable(habit?.id) { mutableStateOf(habit?.name.orEmpty()) }
    var recurrenceName by rememberSaveable(habit?.id) { mutableStateOf((habit?.recurrenceType ?: RecurrenceType.DAILY).name) }
    val recurrence = RecurrenceType.valueOf(recurrenceName)
    var target by rememberSaveable(habit?.id) { mutableStateOf((habit?.targetCount ?: 1).toString()) }
    var color by rememberSaveable(habit?.id) { mutableStateOf(habit?.color ?: habitColors.first()) }
    val id = rememberSaveable(habit?.id) { habit?.id ?: kotlin.uuid.Uuid.random().toString() }
    val createdAt = rememberSaveable(habit?.id) { habit?.createdAt ?: Clock.System.now().toEpochMilliseconds() }
    var discard by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val maximum = when(recurrence) { RecurrenceType.DAILY -> 1; RecurrenceType.WEEKLY -> 7; RecurrenceType.MONTHLY -> 31 }
    val count = target.toIntOrNull()
    val pending = operation?.pending == true
    val dirty = name != habit?.name.orEmpty() || recurrence != (habit?.recurrenceType ?: RecurrenceType.DAILY) ||
        target != (habit?.targetCount ?: 1).toString() || color != (habit?.color ?: habitColors.first())
    AuraEditorSheet(stringResource(if (habit == null) Res.string.rd_new_habit else Res.string.edit_habit),
        stringResource(Res.string.rd_close), stringResource(Res.string.rd_save),
        { if (!pending) { if (dirty) discard = true else onDismiss() } },
        { onSave(Habit(id, name.trim(), recurrence, if (recurrence == RecurrenceType.DAILY) 1 else count!!, color, createdAt)) },
        name.isNotBlank() && count != null && count in 1..maximum, pending, protectDismiss = dirty
    ) {
        AuraTextField(name, { name = it }, stringResource(Res.string.habit_name), Modifier.fillMaxWidth(), readOnly = pending)
        Text(stringResource(Res.string.rd_frequency), style = AppTheme.typography.labelLarge)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
            RecurrenceType.entries.forEach { type ->
                AuraSelectionRow(stringResource(when(type) {
                    RecurrenceType.DAILY -> Res.string.daily
                    RecurrenceType.WEEKLY -> Res.string.weekly
                    RecurrenceType.MONTHLY -> Res.string.monthly
                }), type == recurrence, {
                    recurrenceName = type.name
                    val max = when(type) { RecurrenceType.DAILY -> 1; RecurrenceType.WEEKLY -> 7; RecurrenceType.MONTHLY -> 31 }
                    target = (count ?: 1).coerceIn(1,max).toString()
                }, enabled = !pending)
            }
        }
        AnimatedVisibility(recurrence != RecurrenceType.DAILY) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ target = ((count ?: 1) - 1).coerceAtLeast(1).toString() }, enabled = !pending && (count ?: 1) > 1) {
                    Icon(Icons.Outlined.Remove, stringResource(Res.string.rd_decrease))
                }
                AuraTextField(target, { target = it.filter(Char::isDigit).take(2) }, stringResource(Res.string.rd_goal),
                    Modifier.weight(1f), readOnly = pending, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = "1–$maximum")
                IconButton({ target = ((count ?: 0) + 1).coerceAtMost(maximum).toString() }, enabled = !pending && (count ?: 0) < maximum) {
                    Icon(Icons.Outlined.Add, stringResource(Res.string.rd_increase))
                }
            }
        }
        Text(stringResource(Res.string.color_label), style = AppTheme.typography.labelLarge)
        val labels = stringArrayResource(Res.array.rd_colors)
        val choices = habitColors + listOfNotNull(habit?.color?.takeIf { it !in habitColors })
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
            choices.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
                    row.forEach { hex ->
                        val index = habitColors.indexOf(hex)
                        Surface(Modifier.weight(1f), color = if (color == hex) AppTheme.colors.primaryContainer else AppTheme.colors.surfaceVariant,
                            shape = AuraShapes.input) {
                            Column(Modifier.selectable(color == hex, enabled = !pending, role = Role.RadioButton, onClick = { color = hex })
                                .padding(vertical = AuraSpacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(24.dp).background(parseHexColor(hex), CircleShape))
                                Text(if (index >= 0) labels[index] else stringResource(Res.string.rd_color_current),
                                    style = AppTheme.typography.labelMedium)
                                if (color == hex) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp))
                                else Spacer(Modifier.size(16.dp))
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        OperationNotice(operation)
        if (onDelete != null) TextButton({ delete = true }, enabled = !pending) {
            Text(stringResource(Res.string.delete_habit), color = AppTheme.colors.error)
        }
    }
    if (discard) DiscardDialog(onDismiss, { discard = false })
    if (delete) DeleteDialog({ delete = false; onDelete?.invoke() }, { delete = false })
}
