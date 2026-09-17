package com.programovil.aura.journal.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.button.*
import com.programovil.aura.designsystem.components.state.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalDetailScreen(viewModel: JournalDetailViewModel, onNavigateBack: () -> Unit,
    onDelete: (() -> Unit)? = null) {
    val state by viewModel.uiState.collectAsState()
    val operations by viewModel.operations.states.collectAsState()
    val operation = operations["editor"]
    val pending = operation?.pending == true
    var discard by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val back = {
        if (!pending) {
            if (state.dirty) discard = true else { keyboard?.hide(); onNavigateBack() }
        }
    }
    val imeVisible = WindowInsets.isImeVisible
    AuraBackHandler { if (imeVisible) keyboard?.hide() else back() }
    LaunchedEffect(operation?.requestId, operation?.status) {
        if (operation?.status == OperationStatus.Succeeded) {
            keyboard?.hide()
            viewModel.operations.consume("editor", operation.requestId)
            onNavigateBack()
        }
    }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = LocalAuraGutter.current)) {
        Row(Modifier.fillMaxWidth().padding(vertical = AuraSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back, enabled = !pending) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.journal_back)) }
            Text(stringResource(if (state.isNew) Res.string.journal_new_entry else Res.string.journal_edit_entry),
                Modifier.weight(1f), style = AppTheme.typography.labelLarge)
            AuraButton(stringResource(Res.string.rd_save), viewModel::saveEntry,
                enabled = state.title.isNotBlank() && !state.isLoading && state.loadError == null,
                loading = pending, style = AuraButtonStyle.Text)
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AuraSpacing.md)) {
            when {
                state.isLoading -> AuraSkeleton(rows = 3, label = stringResource(Res.string.rd_loading))
                state.loadError != null -> AuraInlineNotice(state.loadError!!.asString(),
                    actionLabel = stringResource(Res.string.rd_retry), onAction = viewModel::retryLoad)
                else -> {
                    state.entry?.let { Text(auraDate(localDate(it.createdAt)),
                        style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary) }
                    val fieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                    TextField(state.title, viewModel::updateTitle,
                        Modifier.fillMaxWidth(), readOnly = pending,
                        placeholder = { Text(stringResource(Res.string.journal_title_hint), style = AppTheme.typography.headlineLarge) },
                        textStyle = AppTheme.typography.headlineLarge, colors = fieldColors, maxLines = 4)
                    TextField(state.content, viewModel::updateContent,
                        Modifier.fillMaxWidth().heightIn(min = AuraSpacing.section * 4), readOnly = pending,
                        placeholder = { Text(stringResource(Res.string.journal_content_hint), style = AppTheme.typography.journal) },
                        textStyle = AppTheme.typography.journal, colors = fieldColors)
                    if (state.dirty) Text(stringResource(Res.string.rd_unsaved),
                        style = AppTheme.typography.labelMedium, color = AppTheme.colors.textSecondary)
                    OperationNotice(operation)
                    if (state.entry != null) TextButton({ delete = true }, enabled = !pending) {
                        Icon(Icons.Outlined.Delete, null)
                        Spacer(Modifier.width(AuraSpacing.xs))
                        Text(stringResource(Res.string.journal_delete), color = AppTheme.colors.error)
                    }
                }
            }
            Spacer(Modifier.height(AuraSpacing.xl))
        }
    }
    if (discard) DiscardDialog({ viewModel.clearDraft(); keyboard?.hide(); onNavigateBack() }, { discard = false })
    if (delete) DeleteDialog({ delete = false; viewModel.deleteEntry() }, { delete = false })
}
