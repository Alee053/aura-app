package com.programovil.aura.domain.repository

import kotlinx.coroutines.flow.Flow
import com.programovil.aura.domain.model.Todo

interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String): Result<Unit>
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}