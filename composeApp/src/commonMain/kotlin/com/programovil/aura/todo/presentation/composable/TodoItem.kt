package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.domain.model.Todo
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.delete
import aura_app.composeapp.generated.resources.due_label
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppTheme.colors.primary,
                    uncheckedColor = AppTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                todo.dueDate?.let { millis ->
                    val date = Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    Text(
                        text = stringResource(Res.string.due_label, date.toString()),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Text(
                    stringResource(Res.string.delete),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.primary
                )
            }
        }
    }
}
