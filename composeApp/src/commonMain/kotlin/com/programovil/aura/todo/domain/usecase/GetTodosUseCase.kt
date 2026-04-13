package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow

class GetTodosUseCase(
    private val repository: TodoRepository
) {
    operator fun invoke(): Flow<Result<List<Todo>>> = repository.getTodos()
}
