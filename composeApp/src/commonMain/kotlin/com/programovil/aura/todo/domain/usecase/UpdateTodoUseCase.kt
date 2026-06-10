package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import io.mockative.Mockable

@Mockable
class UpdateTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todo: Todo): Result<Unit> = repository.updateTodo(todo)
}