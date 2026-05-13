package com.programovil.aura.designsystem.components.input

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme

@Composable
fun BasicInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
        textStyle = AppTheme.typography.bodyMedium.copy(
            color = AppTheme.colors.textPrimary
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTheme.colors.textPrimary,
            unfocusedTextColor = AppTheme.colors.textPrimary,
            disabledTextColor = AppTheme.colors.textPrimary.copy(alpha = 0.38f),
            focusedBorderColor = AppTheme.colors.primary,
            unfocusedBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
            disabledBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.12f),
            focusedLabelColor = AppTheme.colors.primary,
            unfocusedLabelColor = AppTheme.colors.textPrimary.copy(alpha = 0.6f),
            cursorColor = AppTheme.colors.primary,
            selectionColors = TextSelectionColors(
                handleColor = AppTheme.colors.primary,
                backgroundColor = AppTheme.colors.primary.copy(alpha = 0.4f)
            )
        ),
        shape = RoundedCornerShape(8.dp)
    )
}