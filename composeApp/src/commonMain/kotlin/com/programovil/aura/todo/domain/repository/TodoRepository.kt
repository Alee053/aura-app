package com.programovil.aura.todo.domain.repository

import com.programovil.aura.todo.domain.model.Todo
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String, dueDate: Long? = null): Result<Unit>
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}

expect fun createTodoRepository(): TodoRepository
