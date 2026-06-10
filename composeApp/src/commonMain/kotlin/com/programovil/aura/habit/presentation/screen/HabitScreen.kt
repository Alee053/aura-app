package com.programovil.aura.habit.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.habit.presentation.composable.HabitDialog
import com.programovil.aura.habit.presentation.composable.HabitCard
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.presentation.viewmodel.HabitEvent
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCase
import com.programovil.aura.shared.FeatureFlag
import kotlinx.datetime.*
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.habits_title
import aura_app.composeapp.generated.resources.add_habit
import aura_app.composeapp.generated.resources.empty_habits
import aura_app.composeapp.generated.resources.add_first_habit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {},
    viewModel: HabitViewModel = koinInject(),
    getHabitsAccessibilityUseCase: GetHabitsAccessibilityUseCase = koinInject()
) {
    val showHabitsAccessible by getHabitsAccessibilityUseCase()
        .collectAsState(initial = true)
    LaunchedEffect(showHabitsAccessible) { if (!showHabitsAccessible) { onFeatureDisabled() } }

    val uiState by viewModel.uiState.collectAsState()
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val errorMessage = uiState.error?.asString()
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.habits_title),
                            style = AppTheme.typography.headlineSmall
                        )
                        Text(
                            text = today.toString(),
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHabit = null
                    showDialog = true
                },
                containerColor = AppTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_habit),
                    tint = AppTheme.colors.textPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.colors.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.habits, key = { it.habit.id }) { habitItem ->
                        HabitCard(
                            habitWithStatus = habitItem,
                            onToggle = { date ->
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        date
                                    )
                                )
                            },
                            onLongClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }

                    if (uiState.habits.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.empty_habits),
                                        style = AppTheme.typography.bodyMedium,
                                        color = AppTheme.colors.textSecondary
                                    )
                                    PrimaryButton(
                                        text = stringResource(Res.string.add_first_habit),
                                        onClick = {
                                            editingHabit = null
                                            showDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        HabitDialog(
            habit = editingHabit,
            onDismiss = {
                showDialog = false
                editingHabit = null
            },
            onSave = { habit ->
                viewModel.onEvent(HabitEvent.UpdateHabit(habit))
                showDialog = false
                editingHabit = null
            },
            onDelete = editingHabit?.let { habit ->
                {
                    viewModel.onEvent(HabitEvent.DeleteHabit(habit.id))
                    showDialog = false
                    editingHabit = null
                }
            }
        )
    }
}
