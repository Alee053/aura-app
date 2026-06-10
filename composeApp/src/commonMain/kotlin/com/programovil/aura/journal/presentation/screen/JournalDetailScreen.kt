package com.programovil.aura.journal.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.journal_back
import aura_app.composeapp.generated.resources.journal_content_hint
import aura_app.composeapp.generated.resources.journal_delete
import aura_app.composeapp.generated.resources.journal_edit_entry
import aura_app.composeapp.generated.resources.journal_new_entry
import aura_app.composeapp.generated.resources.journal_save
import aura_app.composeapp.generated.resources.journal_title_hint
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.defaultMinSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailScreen(
    viewModel: JournalDetailViewModel,
    onNavigateBack: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isNewEntry = uiState.entry == null

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val errorMessage = uiState.error?.asString()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
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
                        stringResource(
                            if (isNewEntry) Res.string.journal_new_entry
                            else Res.string.journal_edit_entry
                        ),
                        style = AppTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.journal_back),
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                actions = {
                    if (!isNewEntry && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.journal_delete),
                                tint = AppTheme.colors.textSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.saveEntry() },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(Res.string.journal_save),
                            tint = if (uiState.title.isNotBlank()) AppTheme.colors.primary
                            else AppTheme.colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(stringResource(Res.string.journal_title_hint)) },
                singleLine = true,
                textStyle = AppTheme.typography.titleMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.textSecondary,
                    focusedLabelColor = AppTheme.colors.primary,
                    unfocusedLabelColor = AppTheme.colors.textSecondary,
                    cursorColor = AppTheme.colors.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.updateContent(it) },
                label = { Text(stringResource(Res.string.journal_content_hint)) },
                textStyle = AppTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.textSecondary,
                    focusedLabelColor = AppTheme.colors.primary,
                    unfocusedLabelColor = AppTheme.colors.textSecondary,
                    cursorColor = AppTheme.colors.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .defaultMinSize(minHeight = 300.dp),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}