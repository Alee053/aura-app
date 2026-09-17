package com.programovil.aura.shared.presentation.composable

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.overlay.AuraConfirmDialog
import com.programovil.aura.designsystem.components.state.AuraInlineNotice
import com.programovil.aura.shared.presentation.*
import org.jetbrains.compose.resources.stringResource

@Composable fun OperationNotice(operation: UiOperation?) {
    if (operation?.longRunning == true && operation.pending)
        AuraInlineNotice(stringResource(Res.string.rd_pending), isError = false)
    operation?.error?.let { AuraInlineNotice(it.asString()) }
}
@Composable fun DiscardDialog(onDiscard: () -> Unit, onDismiss: () -> Unit) =
    AuraConfirmDialog(stringResource(Res.string.rd_discard_title), stringResource(Res.string.rd_discard_body),
        stringResource(Res.string.rd_discard), stringResource(Res.string.rd_keep_editing),
        onDiscard, onDismiss, destructive = true)

@Composable fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AuraConfirmDialog(stringResource(Res.string.rd_delete_title), stringResource(Res.string.rd_delete_body),
        stringResource(Res.string.delete_action), stringResource(Res.string.cancel),
        onConfirm, onDismiss, destructive = true)

@Composable fun ObserveToggleFeedback(states: Map<String, UiOperation>, operations: UiOperations) {
    val haptic = LocalHapticFeedback.current
    val observedPending = remember { mutableSetOf<Long>() }
    LaunchedEffect(states) {
        states.forEach { (key, value) ->
            if (value.kind == "toggle") {
                if (value.pending) observedPending.add(value.requestId)
                else if (value.status == OperationStatus.Succeeded) {
                    if (observedPending.remove(value.requestId))
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    operations.consume(key, value.requestId)
                }
            }
        }
    }
}
