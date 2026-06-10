package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository
import io.mockative.Mockable

@Mockable
class DeleteTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: String): Result<Unit> = repository.deleteTodo(todoId)
}
