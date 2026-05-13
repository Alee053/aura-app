package com.programovil.aura.settings.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun ThemeCard(
    name: String,
    colors: List<androidx.compose.ui.graphics.Color>,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AppTheme.colors.textPrimary.copy(alpha = 0.1f)),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(colors),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
            Switch(
                checked = isSelected,
                onCheckedChange = { if (it) onSelect() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.colors.textPrimary,
                    checkedTrackColor = AppTheme.colors.primary,
                    uncheckedThumbColor = AppTheme.colors.textPrimary,
                    uncheckedTrackColor = AppTheme.colors.textPrimary.copy(alpha = 0.1f)
                )
            )
        }
    }
}
