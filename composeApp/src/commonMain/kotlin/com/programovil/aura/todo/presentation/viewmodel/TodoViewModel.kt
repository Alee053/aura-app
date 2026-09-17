package com.programovil.aura.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TodoViewModel(
    private val getTodosUseCase: GetTodosUseCase, private val addTodoUseCase: AddTodoUseCase,
    private val updateTodoUseCase: UpdateTodoUseCase, private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase
) : ViewModel() {
    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    private val _error = MutableStateFlow<UiText?>(null)
    private val _loadError = MutableStateFlow<UiText?>(null)
    private val _isLoading = MutableStateFlow(true)
    val todos = _todos.asStateFlow()
    val error = _error.asStateFlow()
    val loadError = _loadError.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val operations = UiOperations(viewModelScope)
    private var loadJob: Job? = null
    private var latestTodos = emptyList<Todo>()
    private val heldRows = mutableMapOf<String, Todo>()
    private fun publishRows() {
        _todos.value = latestTodos.map { heldRows[it.id] ?: it } + heldRows.values.filter { held -> latestTodos.none { it.id == held.id } }
    }
    private fun hold(id: String) { _todos.value.find { it.id == id }?.let { heldRows[id] = it } }
    private fun release(id: String, result: Result<Unit>) {
        val previous = heldRows.remove(id)
        if (result.isFailure && previous != null) latestTodos = latestTodos.filterNot { it.id == id } + previous
        publishRows()
    }
    init { retryLoad() }
    fun retryLoad() {
        loadJob?.cancel()
        _isLoading.value = _todos.value.isEmpty()
        loadJob = viewModelScope.launch {
            getTodosUseCase().catch { emit(Result.failure(it)) }.collect { result ->
                result.onSuccess { latestTodos = it; publishRows(); _loadError.value = null }
                    .onFailure { _loadError.value = ErrorKey.TodoLoad; _error.value = ErrorKey.TodoLoad }
                _isLoading.value = false
            }
        }
    }
    private fun report(result: Result<Unit>, error: UiText) { if (result.isFailure) _error.value = error }
    fun addTodo(title: String, description: String? = null, dueDate: Long? = null) {
        if (title.isBlank()) return
        _error.value = null
        operations.launch("editor", "save", ErrorKey.TodoAdd, { report(it, ErrorKey.TodoAdd) }) {
            addTodoUseCase(title.trim(), description?.trim(), dueDate)
        }
    }
    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        if (operations.isPending("toggle:$todoId")) return
        hold(todoId)
        _error.value = null
        operations.launch("toggle:$todoId", "toggle", ErrorKey.TodoUpdate, { release(todoId, it); report(it, ErrorKey.TodoUpdate) }) {
            toggleTodoUseCase(todoId, isCompleted)
        }
    }
    fun deleteTodo(todoId: String) {
        if (operations.isPending("editor")) return
        hold(todoId)
        _error.value = null
        operations.launch("editor", "delete", ErrorKey.TodoDelete, { release(todoId, it); report(it, ErrorKey.TodoDelete) }) {
            deleteTodoUseCase(todoId)
        }
    }
    fun updateTodo(todo: Todo) {
        if (operations.isPending("editor")) return
        hold(todo.id)
        _error.value = null
        operations.launch("editor", "save", ErrorKey.TodoUpdate, { release(todo.id, it); report(it, ErrorKey.TodoUpdate) }) {
            updateTodoUseCase(todo)
        }
    }
    fun clearError() { _error.value = null }
}
