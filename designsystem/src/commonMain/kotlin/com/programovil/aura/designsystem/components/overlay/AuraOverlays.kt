package com.programovil.aura.designsystem.components.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.*
import com.programovil.aura.designsystem.theme.*

@Stable
class AuraOverlayRegistry {
    var count by mutableIntStateOf(0)
        private set
    fun enter() { count++ }
    fun leave() { count = (count - 1).coerceAtLeast(0) }
}
val LocalAuraOverlays = staticCompositionLocalOf { AuraOverlayRegistry() }

@Composable
fun RegisterAuraOverlay() {
    val registry = LocalAuraOverlays.current
    DisposableEffect(registry) { registry.enter(); onDispose { registry.leave() } }
}

@Composable
fun AuraConfirmDialog(
    title: String, message: String, confirmLabel: String, cancelLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false
) {
    RegisterAuraOverlay()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Text(message) }, containerColor = AppTheme.colors.surface,
        confirmButton = {
            AuraButton(confirmLabel, onConfirm,
                style = if (destructive) AuraButtonStyle.Destructive else AuraButtonStyle.Primary)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraEditorSheet(
    title: String, closeLabel: String, saveLabel: String, onDismiss: () -> Unit,
    onSave: () -> Unit, canSave: Boolean, pending: Boolean,
    protectDismiss: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    RegisterAuraOverlay()
    // A dirty draft may ask for confirmation without the sheet first disappearing.
    val latestPending by rememberUpdatedState(pending)
    val latestProtect by rememberUpdatedState(protectDismiss)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && (latestPending || latestProtect)) {
                if (!latestPending) latestDismiss()
                false
            } else true
        })
    ModalBottomSheet(
        onDismissRequest = { if (!pending) onDismiss() }, sheetState = sheetState,
        sheetMaxWidth = AuraSpacing.editorWidth,
        shape = AuraShapes.sheet, containerColor = AppTheme.colors.surface,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = .4f),
        dragHandle = null
    ) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = AuraSpacing.lg)) {
            Row(Modifier.fillMaxWidth().padding(vertical = AuraSpacing.md),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = AppTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss, enabled = !pending) { Text(closeLabel) }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AuraSpacing.md), content = content)
            AuraButton(saveLabel, onSave,
                Modifier.fillMaxWidth().padding(vertical = AuraSpacing.md),
                enabled = canSave, loading = pending)
        }
    }
}
