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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.habit.presentation.composable.AddHabitDialog
import com.programovil.aura.habit.presentation.composable.HabitItem
import com.programovil.aura.theme.AppTheme
import com.programovil.aura.habit.presentation.viewmodel.HabitEvent
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import kotlinx.datetime.*
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.habits_title
import aura_app.composeapp.generated.resources.sign_out
import aura_app.composeapp.generated.resources.add_habit
import aura_app.composeapp.generated.resources.today
import aura_app.composeapp.generated.resources.tomorrow
import aura_app.composeapp.generated.resources.this_week
import aura_app.composeapp.generated.resources.empty_habits
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = koinInject(),
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.habits_title))
                        Text(
                            text = today.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_habit))
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(Res.string.sign_out))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Today section
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
                            }
                        )
                    }
                }

                // Tomorrow section
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
                            }
                        )
                    }
                }

                // This Week section
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
                            }
                        )
                    }
                }

                // Empty state
                if (uiState.todayHabits.isEmpty() &&
                    uiState.tomorrowHabits.isEmpty() &&
                    uiState.thisWeekHabits.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.empty_habits),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, recurrenceType, daysOfWeek, color ->
                viewModel.onEvent(HabitEvent.AddHabit(name, recurrenceType, daysOfWeek, color))
                showAddDialog = false
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
