package com.programovil.aura.journal.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.header.AuraScreenHeader
import com.programovil.aura.designsystem.components.state.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.presentation.composable.JournalCard
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel, onNavigateToDetail: (String?) -> Unit,
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(), onFeatureDisabled: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val operations by viewModel.operations.states.collectAsState()
    var deleting by remember { mutableStateOf<JournalEntry?>(null) }
    Scaffold(containerColor = AppTheme.colors.background, contentWindowInsets = WindowInsets(0,0,0,0),
        floatingActionButton = {
            if (state.entries.isNotEmpty()) ExtendedFloatingActionButton(onClick = { onNavigateToDetail(null) },
                icon = { Icon(Icons.Outlined.Edit, null) }, text = { Text(stringResource(Res.string.rd_write)) },
                containerColor = AppTheme.colors.primary, contentColor = AppTheme.colors.onPrimary)
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = LocalAuraGutter.current),
            contentPadding = PaddingValues(bottom = AuraSpacing.section + AuraSpacing.xl)) {
            item { AuraScreenHeader(stringResource(Res.string.journal_title), stringResource(Res.string.rd_journal_intro)) }
            state.loadError?.let { error -> item {
                AuraInlineNotice(error.asString(), actionLabel = stringResource(Res.string.rd_retry), onAction = viewModel::retryLoad)
            } }
            operations.values.firstOrNull { it.status == OperationStatus.Failed }?.let { op -> item {
                AuraInlineNotice(op.error!!.asString(), actionLabel = stringResource(Res.string.rd_close),
                    onAction = { viewModel.operations.consume(op.target, op.requestId) })
            } }
            operations.values.firstOrNull { it.pending && it.longRunning }?.let { op ->
                item { OperationNotice(op) }
            }
            when {
                state.isLoading -> item { AuraSkeleton(label = stringResource(Res.string.rd_loading)) }
                state.entries.isEmpty() && state.loadError == null -> item {
                    AuraEmptyState(stringResource(Res.string.rd_empty_journal), stringResource(Res.string.rd_empty_journal_body),
                        stringResource(Res.string.rd_write), { onNavigateToDetail(null) },
                        illustration = { AuraScene(3, Modifier.size(AuraSpacing.control * 2)) })
                }
                else -> state.entries.groupBy { localDate(it.createdAt).let { date -> date.year to date.monthNumber } }.forEach { (_, entries) ->
                    item(key = "month:${entries.first().id}") {
                        Text(auraMonth(localDate(entries.first().createdAt)), Modifier.padding(top = AuraSpacing.lg, bottom = AuraSpacing.xs),
                            style = AppTheme.typography.labelLarge, color = AppTheme.colors.primary)
                    }
                    items(entries, key = { it.id }) { entry ->
                        val pending = operations[entry.id]?.pending == true
                        val swipe = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && !pending) deleting = entry
                            false
                        })
                        SwipeToDismissBox(swipe, backgroundContent = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Outlined.Delete, stringResource(Res.string.delete_action), tint = AppTheme.colors.error)
                            }
                        }, enableDismissFromStartToEnd = false, enableDismissFromEndToStart = !pending,
                            modifier = Modifier.animateItem()) {
                            Surface(color = AppTheme.colors.background) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    JournalCard(entry, { if (!pending) onNavigateToDetail(entry.id) }, Modifier.weight(1f))
                                    if (pending) CircularProgressIndicator(Modifier.size(AuraSpacing.lg))
                                    else IconButton({ deleting = entry }) { Icon(Icons.Outlined.Delete, stringResource(Res.string.journal_delete)) }
                                }
                            }
                        }
                        HorizontalDivider(color = AppTheme.colors.outline)
                    }
                }
            }
        }
    }
    deleting?.let { entry -> DeleteDialog({ deleting = null; viewModel.deleteEntry(entry) }, { deleting = null }) }
}
