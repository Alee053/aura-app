package com.programovil.aura.todo.domain.repository

import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createTodoRepository(): TodoRepository = IosTodoRepositoryImpl()

private class IosTodoRepositoryImpl : TodoRepository {
    override fun getTodos(): Flow<Result<List<Todo>>> = flowOf(Result.success(emptyList()))
    override suspend fun addTodo(title: String): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTodo(todoId: String): Result<Unit> = Result.success(Unit)
}
