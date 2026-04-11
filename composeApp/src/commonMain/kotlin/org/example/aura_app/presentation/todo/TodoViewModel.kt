package org.example.aura_app.presentation.todo

import androidx.lifecycle.ViewModel
import org.example.aura_app.domain.model.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodoViewModel : ViewModel() {
    private val _todos = MutableStateFlow(
        listOf(
            Todo(id = "1", title = "Buy groceries", isCompleted = false),
            Todo(id = "2", title = "Read a book", isCompleted = true),
            Todo(id = "3", title = "Exercise", isCompleted = false)
        )
    )
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()
}