package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class ToggleTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: String, isCompleted: Boolean): Result<Unit> =
        repository.toggleTodo(todoId, isCompleted)
}
