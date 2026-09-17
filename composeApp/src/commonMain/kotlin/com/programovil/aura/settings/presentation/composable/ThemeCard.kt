package com.programovil.aura.settings.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.*

@Composable
fun ThemeCard(name: String, colors: List<Color>, isSelected: Boolean, onSelect: () -> Unit) {
    Surface(color = AppTheme.colors.surface, shape = AuraShapes.card,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) AppTheme.colors.primary else AppTheme.colors.outline)) {
        Column(Modifier.fillMaxWidth().selectable(isSelected, role = Role.RadioButton, onClick = onSelect).padding(AuraSpacing.md)) {
            Surface(shape = AuraShapes.input, color = colors.first()) {
                Column(Modifier.fillMaxWidth().height(72.dp).padding(AuraSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
                    Box(Modifier.fillMaxWidth(.7f).height(12.dp).background(colors.last(), AuraShapes.badge))
                    Box(Modifier.fillMaxWidth(.45f).height(6.dp).background(colors.last(), AuraShapes.badge))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = AuraSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), style = AppTheme.typography.labelLarge)
                RadioButton(isSelected, null)
            }
        }
    }
}
