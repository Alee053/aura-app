package com.programovil.aura.todo.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.presentation.composable.TodoItem
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.add_todo
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.empty_todos
import aura_app.composeapp.generated.resources.new_todo_hint
import aura_app.composeapp.generated.resources.ok
import aura_app.composeapp.generated.resources.select_due_date
import aura_app.composeapp.generated.resources.todos_title
import aura_app.composeapp.generated.resources.due_date_label
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel
) {
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var newTodoTitle by remember { mutableStateOf("") }
    var selectedDueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.todos_title),
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
                onClick = {
                    if (newTodoTitle.isNotBlank()) {
                        viewModel.addTodo(newTodoTitle, selectedDueDate)
                        newTodoTitle = ""
                        selectedDueDate = null
                    }
                },
                containerColor = AppTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_todo),
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
            OutlinedTextField(
                value = newTodoTitle,
                onValueChange = { newTodoTitle = it },
                label = { Text(stringResource(Res.string.new_todo_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                textStyle = AppTheme.typography.bodyMedium.copy(
                    color = AppTheme.colors.textPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                    disabledTextColor = AppTheme.colors.textPrimary.copy(alpha = 0.38f),
                    focusedBorderColor = AppTheme.colors.primary,
                    unfocusedBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
                    disabledBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.12f),
                    focusedLabelColor = AppTheme.colors.primary,
                    unfocusedLabelColor = AppTheme.colors.textSecondary,
                    cursorColor = AppTheme.colors.primary
                ),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(Res.string.select_due_date),
                            tint = if (selectedDueDate != null) AppTheme.colors.primary else AppTheme.colors.textSecondary
                        )
                    }
                }
            )

            selectedDueDate?.let { millis ->
                val date = Instant.fromEpochMilliseconds(millis)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                Text(
                    text = stringResource(Res.string.due_date_label, date.toString()),
                    style = AppTheme.typography.labelLarge,
                    color = AppTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }
                todos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(Res.string.empty_todos),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(todos, key = { it.id }) { todo ->
                            TodoItem(
                                todo = todo,
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onDelete = { viewModel.deleteTodo(todo.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
