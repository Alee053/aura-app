package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.input.AuraTextField
import com.programovil.aura.designsystem.components.overlay.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import com.programovil.aura.todo.domain.model.Todo
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDialog(todo: Todo?, onDismiss: () -> Unit,
    onSave: (String, String?, Long?) -> Unit, onDelete: (() -> Unit)? = null,
    operation: UiOperation? = null) {
    var title by rememberSaveable(todo?.id) { mutableStateOf(todo?.title.orEmpty()) }
    var description by rememberSaveable(todo?.id) { mutableStateOf(todo?.description.orEmpty()) }
    var dueDate by rememberSaveable(todo?.id) { mutableStateOf(todo?.dueDate) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val dirty = title != todo?.title.orEmpty() || description != todo?.description.orEmpty() || dueDate != todo?.dueDate
    val pending = operation?.pending == true
    val close = { if (!pending) { if (dirty) discard = true else onDismiss() } }
    AuraEditorSheet(
        stringResource(if (todo == null) Res.string.rd_new_task else Res.string.edit_todo),
        stringResource(Res.string.rd_close), stringResource(Res.string.rd_save), close,
        { onSave(title.trim(), description.takeIf { it.isNotBlank() }?.trim(), dueDate) },
        title.isNotBlank(), pending, protectDismiss = dirty
    ) {
        AuraTextField(title, { title = it }, stringResource(Res.string.todo_name_hint), Modifier.fillMaxWidth(), readOnly = pending)
        AuraTextField(description, { description = it }, stringResource(Res.string.todo_description_hint),
            Modifier.fillMaxWidth(), readOnly = pending, singleLine = false, minLines = 3, maxLines = 6)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton({ datePicker = true }, Modifier.weight(1f).heightIn(min = AuraSpacing.control),
                enabled = !pending, shape = AuraShapes.input) {
                Text(dueDate?.let { auraDate(localDate(it)) } ?: stringResource(Res.string.add_due_date))
            }
            if (dueDate != null) IconButton({ dueDate = null }, enabled = !pending) {
                Icon(Icons.Outlined.Close, stringResource(Res.string.clear_due_date))
            }
        }
        OperationNotice(operation)
        if (onDelete != null) TextButton({ delete = true }, enabled = !pending) {
            Text(stringResource(Res.string.delete_action), color = AppTheme.colors.error)
        }
    }
    if (datePicker) {
        RegisterAuraOverlay()
        val picker = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(onDismissRequest = { datePicker = false },
            confirmButton = { TextButton({ dueDate = picker.selectedDateMillis; datePicker = false }) { Text(stringResource(Res.string.rd_save)) } },
            dismissButton = { TextButton({ datePicker = false }) { Text(stringResource(Res.string.cancel)) } }) { DatePicker(picker) }
    }
    if (discard) DiscardDialog(onDismiss, { discard = false })
    if (delete) DeleteDialog({ delete = false; onDelete?.invoke() }, { delete = false })
}
