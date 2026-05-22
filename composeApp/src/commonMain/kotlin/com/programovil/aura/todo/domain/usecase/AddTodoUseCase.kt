package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class AddTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(title: String, description: String? = null, dueDate: Long? = null): Result<Unit> = repository.addTodo(title, description, dueDate)
}
