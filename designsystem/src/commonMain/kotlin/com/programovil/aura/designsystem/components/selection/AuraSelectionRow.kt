package com.programovil.aura.designsystem.components.selection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.Role
import com.programovil.aura.designsystem.theme.*

@Composable
fun AuraSelectionRow(label: String, selected: Boolean, onSelect: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(color = if (selected) AppTheme.colors.primaryContainer else AppTheme.colors.surface,
        shape = AuraShapes.input) {
        Row(modifier.fillMaxWidth().heightIn(min = AuraSpacing.touch)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = AuraSpacing.md, vertical = AuraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = AppTheme.typography.labelLarge)
            RadioButton(selected, onClick = null, enabled = enabled)
        }
    }
}
