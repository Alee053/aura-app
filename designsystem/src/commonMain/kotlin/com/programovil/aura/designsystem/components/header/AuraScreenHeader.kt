package com.programovil.aura.designsystem.components.header

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import com.programovil.aura.designsystem.theme.*

@Composable
fun AuraScreenHeader(title: String, subtitle: String? = null,
    modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = AuraSpacing.lg, bottom = AuraSpacing.lg),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
            Text(title, style = AppTheme.typography.headlineLarge, color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary) }
        }
        actions()
    }
}
