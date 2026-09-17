package com.programovil.aura.settings.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.*

@Composable
fun PreferenceItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = AppTheme.colors.surface, shape = AuraShapes.card,
        border = if (AppTheme.colors.highContrast) BorderStroke(1.dp, AppTheme.colors.outline) else null) {
        Row(Modifier.toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(AuraSpacing.card), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = AppTheme.typography.bodyLarge)
                Spacer(Modifier.height(AuraSpacing.xxs))
                Text(subtitle, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
            }
            Spacer(Modifier.width(AuraSpacing.sm))
            Switch(checked, onCheckedChange = null)
        }
    }
}
