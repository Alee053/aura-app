package com.programovil.aura.todo.presentation.screen

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.presentation.composable.TodoDialog
import com.programovil.aura.todo.presentation.composable.TodoItem
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.add_first_todo
import aura_app.composeapp.generated.resources.add_todo
import aura_app.composeapp.generated.resources.completed_section
import aura_app.composeapp.generated.resources.empty_todos
import aura_app.composeapp.generated.resources.todos_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel
) {
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDialog) {
        TodoDialog(
            todo = editingTodo,
            onDismiss = {
                showDialog = false
                editingTodo = null
            },
            onSave = { title, description, dueDate ->
                if (editingTodo != null) {
                    viewModel.updateTodo(
                        editingTodo!!.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate
                        )
                    )
                } else {
                    viewModel.addTodo(title, description, dueDate)
                }
            },
            onDelete = editingTodo?.let { todo ->
                {
                    viewModel.deleteTodo(todo.id)
                }
            }
        )
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
                    editingTodo = null
                    showDialog = true
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
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }
                todos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(Res.string.empty_todos),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary
                            )
                            PrimaryButton(
                                text = stringResource(Res.string.add_first_todo),
                                onClick = {
                                    editingTodo = null
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
                else -> {
                    val activeTodos = todos.filter { !it.isCompleted }
                    val completedTodos = todos.filter { it.isCompleted }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        items(activeTodos, key = { it.id }) { todo ->
                            TodoItem(
                                todo = todo,
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onClick = {
                                    editingTodo = todo
                                    showDialog = true
                                }
                            )
                        }

                        if (completedTodos.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.completed_section),
                                    style = AppTheme.typography.titleMedium,
                                    color = AppTheme.colors.textSecondary,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                            items(completedTodos, key = { it.id }) { todo ->
                                TodoItem(
                                    todo = todo,
                                    onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                    onClick = {
                                        editingTodo = todo
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