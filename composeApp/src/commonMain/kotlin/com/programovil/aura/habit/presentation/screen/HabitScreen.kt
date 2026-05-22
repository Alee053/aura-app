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
import com.programovil.aura.habit.presentation.composable.HabitItem
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.presentation.viewmodel.HabitEvent
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import kotlinx.datetime.*
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.habits_title
import aura_app.composeapp.generated.resources.add_habit
import aura_app.composeapp.generated.resources.today
import aura_app.composeapp.generated.resources.tomorrow
import aura_app.composeapp.generated.resources.this_week
import aura_app.composeapp.generated.resources.empty_habits
import aura_app.composeapp.generated.resources.add_first_habit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
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
                        Text(stringResource(Res.string.habits_title))
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (uiState.todayHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.today),
                            subtitle = today.toString()
                        )
                    }
                    items(uiState.todayHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            },
                            onLongClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }
                }

                if (uiState.tomorrowHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.tomorrow),
                            subtitle = tomorrow.toString()
                        )
                    }
                    items(uiState.tomorrowHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            },
                            onLongClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }
                }

                if (uiState.thisWeekHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.this_week),
                            subtitle = null
                        )
                    }
                    items(uiState.thisWeekHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            },
                            onLongClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }
                }

                if (uiState.todayHabits.isEmpty() &&
                    uiState.tomorrowHabits.isEmpty() &&
                    uiState.thisWeekHabits.isEmpty()) {
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

@Composable
private fun HabitSectionHeader(title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}
