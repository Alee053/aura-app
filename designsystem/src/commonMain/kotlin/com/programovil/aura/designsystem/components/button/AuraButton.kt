package com.programovil.aura.designsystem.components.button

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.*

enum class AuraButtonStyle { Primary, Tonal, Text, Destructive }

@Composable
fun AuraButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false,
    style: AuraButtonStyle = AuraButtonStyle.Primary,
    loadingLabel: String = text
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled && !loading) .98f else 1f,
        tween(LocalAuraMotion.current.interaction), label = "button pressure")
    val c = AppTheme.colors
    val container = when (style) {
        AuraButtonStyle.Primary -> c.primary
        AuraButtonStyle.Tonal -> c.primaryContainer
        AuraButtonStyle.Destructive -> c.error
        AuraButtonStyle.Text -> androidx.compose.ui.graphics.Color.Transparent
    }
    val ink = when (style) {
        AuraButtonStyle.Primary -> c.onPrimary
        AuraButtonStyle.Tonal -> c.onPrimaryContainer
        AuraButtonStyle.Destructive -> c.onError
        AuraButtonStyle.Text -> c.primary
    }
    Button(
        onClick = onClick, enabled = enabled && !loading,
        interactionSource = interaction,
        modifier = modifier.heightIn(min = AuraSpacing.control).graphicsLayer { scaleX = scale; scaleY = scale }.semantics {
            if (loading) { stateDescription = loadingLabel; liveRegion = LiveRegionMode.Polite }
        },
        shape = AuraShapes.button,
        contentPadding = PaddingValues(horizontal = AuraSpacing.lg, vertical = AuraSpacing.md),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = ink,
            disabledContainerColor = if (loading) container else c.surfaceVariant,
            disabledContentColor = if (loading) ink else c.textSecondary)
    ) {
        // The label stays in layout, preserving width during an asynchronous operation.
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text, style = AppTheme.typography.labelLarge,
                color = if (loading) ink.copy(alpha = 0f) else if (enabled) ink else c.textSecondary)
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = ink, strokeWidth = 2.dp)
        }
    }
}
