package com.programovil.aura.journal.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.shared.presentation.*

@Composable
fun JournalCard(entry: JournalEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val date = localDate(entry.createdAt)
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = AuraSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
        Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.dayOfMonth.toString(), style = AppTheme.typography.headlineSmall, color = AppTheme.colors.primary)
            Text(auraDay(date), style = AppTheme.typography.labelMedium, color = AppTheme.colors.textSecondary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
            Text(entry.title, style = AppTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.content.isNotBlank()) Text(entry.content, style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
