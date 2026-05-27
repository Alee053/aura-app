package com.programovil.aura.journal.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.journal.presentation.composable.JournalCard
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
import com.programovil.aura.shared.FeatureFlag
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.journal_empty
import aura_app.composeapp.generated.resources.journal_empty_subtitle
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onNavigateToDetail: (String?) -> Unit,
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {}
) {
    LaunchedEffect(featureFlags) {
        if (featureFlags[FeatureFlag.JOURNAL_ENABLED] == false) {
            onFeatureDisabled()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.journal_title),
                        style = AppTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDetail(null) },
                containerColor = AppTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.journal_add_entry),
                    tint = AppTheme.colors.textPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }
                uiState.entries.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(Res.string.journal_empty),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                stringResource(Res.string.journal_empty_subtitle),
                                style = AppTheme.typography.labelLarge,
                                color = AppTheme.colors.textSecondary
                            )
                            PrimaryButton(
                                text = stringResource(Res.string.journal_add_entry),
                                onClick = { onNavigateToDetail(null) }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        items(uiState.entries, key = { it.id }) { entry ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteEntry(entry)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.journal_delete),
                                            color = AppTheme.colors.error,
                                            style = AppTheme.typography.labelLarge,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                JournalCard(
                                    entry = entry,
                                    onClick = { onNavigateToDetail(entry.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}