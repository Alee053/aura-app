package com.programovil.aura.designsystem.components.state

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.AuraButton
import com.programovil.aura.designsystem.components.button.AuraButtonStyle
import com.programovil.aura.designsystem.theme.*
import kotlinx.coroutines.delay

@Composable
fun AuraInlineNotice(message: String, modifier: Modifier = Modifier,
    actionLabel: String? = null, onAction: (() -> Unit)? = null, isError: Boolean = true) {
    Surface(modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = AuraShapes.input,
        color = if (isError) AppTheme.colors.errorContainer else AppTheme.colors.primaryContainer) {
        Column(Modifier.padding(AuraSpacing.md)) {
            Text(message, style = AppTheme.typography.bodyMedium,
                color = if (isError) AppTheme.colors.error else AppTheme.colors.onPrimaryContainer)
            if (onAction != null && actionLabel != null)
                AuraButton(actionLabel, onAction, style = AuraButtonStyle.Text)
        }
    }
}

@Composable
fun AuraEmptyState(title: String, description: String, actionLabel: String, onAction: () -> Unit,
    modifier: Modifier = Modifier, illustration: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(vertical = AuraSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
        illustration()
        Text(title, style = AppTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(description, style = AppTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center)
        AuraButton(actionLabel, onAction)
    }
}

@Composable
fun AuraSkeleton(modifier: Modifier = Modifier, rows: Int = 3, label: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); visible = true }
    if (visible) {
        val motion = LocalAuraMotion.current
        val alpha = if (motion.enabled && !AppTheme.colors.highContrast) {
            val transition = rememberInfiniteTransition(label = "skeleton")
            val value by transition.animateFloat(.45f, .85f,
                infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "opacity")
            value
        } else 1f
        Column(modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }, verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
            repeat(rows.coerceIn(1, 4)) {
                Box(Modifier.fillMaxWidth().height(88.dp).clip(AuraShapes.input)
                    .background(AppTheme.colors.surfaceVariant.copy(alpha = alpha)))
            }
        }
    } else Spacer(modifier.fillMaxWidth().height(80.dp))
}
