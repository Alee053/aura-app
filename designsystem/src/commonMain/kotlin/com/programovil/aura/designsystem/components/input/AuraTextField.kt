package com.programovil.aura.designsystem.components.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.programovil.aura.designsystem.theme.*

@Composable
fun AuraTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true,
    error: String? = null, supportingText: String? = null,
    minLines: Int = 1, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value, onValueChange, modifier.heightIn(min = AuraSpacing.control),
        enabled = enabled, readOnly = readOnly, singleLine = singleLine, minLines = minLines, maxLines = maxLines,
        label = { Text(label) }, isError = error != null,
        supportingText = if (error != null || supportingText != null) {
            { Text(error ?: supportingText.orEmpty()) }
        } else null,
        textStyle = AppTheme.typography.bodyLarge,
        shape = AuraShapes.input, keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppTheme.colors.surface,
            unfocusedContainerColor = AppTheme.colors.surface,
            focusedBorderColor = AppTheme.colors.primary,
            unfocusedBorderColor = AppTheme.colors.controlOutline
        )
    )
}
