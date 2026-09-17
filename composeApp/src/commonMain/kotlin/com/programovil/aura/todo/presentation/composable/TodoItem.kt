package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.shared.presentation.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TodoItem(todo: Todo, onToggle: () -> Unit, onClick: () -> Unit,
    modifier: Modifier = Modifier, pending: Boolean = false) {
    Surface(color = AppTheme.colors.surface, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = AuraSpacing.sm, horizontal = AuraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(AuraSpacing.touch), contentAlignment = Alignment.Center) {
                if (pending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Checkbox(todo.isCompleted, { onToggle() })
            }
            Column(Modifier.weight(1f).clickable(enabled = !pending, onClickLabel = stringResource(Res.string.rd_edit), onClick = onClick)
                .padding(vertical = AuraSpacing.xs, horizontal = AuraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.xxs)) {
                Text(todo.title, style = AppTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (todo.isCompleted) AppTheme.colors.textSecondary else AppTheme.colors.textPrimary,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null)
                todo.dueDate?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xxs)) {
                        Icon(Icons.Outlined.Event, null, Modifier.size(16.dp), tint = AppTheme.colors.textSecondary)
                        Text(auraDate(localDate(it)), style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
                    }
                }
                todo.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
