package com.programovil.aura.presentation.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.programovil.aura.domain.model.Todo
import com.programovil.aura.domain.repository.TodoRepository

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    val todos: StateFlow<List<Todo>> = _todos
    val error: StateFlow<String?> = _error
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            repository.getTodos().collect { result ->
                result.onSuccess { _todos.value = it }
                    .onFailure { _error.value = it.message ?: "Unknown error" }
                _isLoading.value = false
            }
        }
    }

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTodo(title.trim())
                .onFailure { _error.value = "Failed to add todo" }
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTodo(todoId, isCompleted)
                .onFailure { _error.value = "Failed to update todo" }
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            repository.deleteTodo(todoId)
                .onFailure { _error.value = "Failed to delete todo" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
