package com.programovil.aura.todo.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.header.AuraScreenHeader
import com.programovil.aura.designsystem.components.state.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.presentation.composable.*
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun TodoScreen(viewModel: TodoViewModel, featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {}) {
    val todos by viewModel.todos.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val operations by viewModel.operations.states.collectAsState()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var completedExpanded by rememberSaveable { mutableStateOf(true) }
    val editor = operations["editor"]
    // Keep the edited entity stable while snapshots update optimistically.
    var editingTodo by rememberSaveable(stateSaver = TodoEditorSaver) { mutableStateOf<Todo?>(null) }
    val selected = editingTodo ?: todos.find { it.id == editingId }
    val openNew = { viewModel.operations.clearCompleted(); editingId = null; editingTodo = null; showEditor = true }
    ObserveToggleFeedback(operations, viewModel.operations)
    LaunchedEffect(editor?.requestId, editor?.status) {
        if (editor?.status == OperationStatus.Succeeded) {
            showEditor = false; editingTodo = null; editingId = null
            viewModel.operations.consume("editor", editor.requestId)
        }
    }
    val pendingSnapshot = remember { mutableStateMapOf<String, Todo>() }
    // Hold a row in its current group until the mutation result is confirmed.
    fun toggle(todo: Todo) {
        pendingSnapshot[todo.id] = todo
        viewModel.toggleTodo(todo.id, !todo.isCompleted)
    }
    LaunchedEffect(operations) {
        pendingSnapshot.keys.toList().forEach { id ->
            if (operations["toggle:$id"]?.pending != true) pendingSnapshot.remove(id)
        }
    }
    val visibleTodos = todos.map { pendingSnapshot[it.id] ?: it }
    Scaffold(containerColor = AppTheme.colors.background, contentWindowInsets = WindowInsets(0,0,0,0),
        floatingActionButton = {
            if (todos.isNotEmpty()) ExtendedFloatingActionButton(onClick = openNew,
                icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(stringResource(Res.string.rd_new_task)) },
                containerColor = AppTheme.colors.primary, contentColor = AppTheme.colors.onPrimary)
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = LocalAuraGutter.current),
            contentPadding = PaddingValues(bottom = AuraSpacing.section + AuraSpacing.xl)) {
            item { AuraScreenHeader(stringResource(Res.string.todos_title), stringResource(Res.string.rd_todos_intro)) }
            loadError?.let { error -> item {
                AuraInlineNotice(error.asString(), actionLabel = stringResource(Res.string.rd_retry), onAction = viewModel::retryLoad)
            } }
            operations.values.firstOrNull { it.kind == "toggle" && it.status == OperationStatus.Failed }?.let { operation ->
                item { AuraInlineNotice(operation.error!!.asString(), actionLabel = stringResource(Res.string.rd_close),
                    onAction = { viewModel.operations.consume(operation.target, operation.requestId) }) }
            }
            operations.values.firstOrNull { it.pending && it.longRunning }?.let { op ->
                item { OperationNotice(op) }
            }
            when {
                loading -> item { AuraSkeleton(label = stringResource(Res.string.rd_loading)) }
                todos.isEmpty() && loadError == null -> item {
                    AuraEmptyState(stringResource(Res.string.rd_empty_tasks), stringResource(Res.string.rd_empty_tasks_body),
                        stringResource(Res.string.rd_new_task), openNew,
                        illustration = { AuraScene(1, Modifier.size(AuraSpacing.control * 2)) })
                }
                else -> {
                    item { Text(stringResource(Res.string.rd_active), Modifier.padding(vertical = AuraSpacing.md),
                        style = AppTheme.typography.labelLarge, color = AppTheme.colors.textSecondary) }
                    items(visibleTodos.filter { !it.isCompleted }, key = { it.id }) { todo ->
                        TodoItem(todo, { toggle(todo) }, {
                            viewModel.operations.clearCompleted(); editingId = todo.id; editingTodo = todo; showEditor = true
                        }, Modifier.animateItem(), operations["toggle:${todo.id}"]?.pending == true)
                        HorizontalDivider(color = AppTheme.colors.outline)
                    }
                    if (visibleTodos.any { it.isCompleted }) item {
                        TextButton({ completedExpanded = !completedExpanded }, Modifier.fillMaxWidth().padding(top = AuraSpacing.md)) {
                            Text("${stringResource(Res.string.rd_done)} · ${visibleTodos.count { it.isCompleted }}")
                        }
                    }
                    if (completedExpanded) items(visibleTodos.filter { it.isCompleted }, key = { it.id }) { todo ->
                        TodoItem(todo, { toggle(todo) }, {
                            viewModel.operations.clearCompleted(); editingId = todo.id; editingTodo = todo; showEditor = true
                        }, Modifier.animateItem(), operations["toggle:${todo.id}"]?.pending == true)
                        HorizontalDivider(color = AppTheme.colors.outline)
                    }
                }
            }
        }
    }
    if (showEditor) TodoDialog(selected,
        onDismiss = { showEditor = false; editingTodo = null; editingId = null },
        onSave = { title, description, date ->
            if (selected == null) viewModel.addTodo(title, description, date)
            else viewModel.updateTodo(selected.copy(title = title, description = description, dueDate = date))
        },
        onDelete = selected?.let { { viewModel.deleteTodo(it.id) } }, operation = editor)
}
