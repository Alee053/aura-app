package com.programovil.aura.todo.domain.repository

import com.google.firebase.firestore.FieldValue
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.todo.data.mapper.TodoData
import com.programovil.aura.todo.data.mapper.toDomain
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual fun createTodoRepository(): TodoRepository = TodoRepositoryImpl()

private class TodoRepositoryImpl : TodoRepository {

    private val userId: String
        get() = FirebaseConfig.auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userTodosCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("todos")

    override fun getTodos(): Flow<Result<List<Todo>>> = callbackFlow {
        val listener = userTodosCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val todos = snapshot?.documents?.mapNotNull { doc ->
                    val todoData = TodoData(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description"),
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        dueDate = doc.getLong("dueDate")
                    )
                    todoData.toDomain()
                } ?: emptyList<Todo>()
                trySend(Result.success(todos))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addTodo(title: String, description: String?, dueDate: Long?): Result<Unit> = runCatching {
        val data = mutableMapOf(
            "title" to title,
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (description != null) {
            data["description"] = description
        }
        if (dueDate != null) {
            data["dueDate"] = dueDate
        }
        userTodosCollection().add(data).await()
    }

    override suspend fun updateTodo(todo: Todo): Result<Unit> = runCatching {
        val data = mutableMapOf<String, Any>(
            "title" to todo.title,
            "isCompleted" to todo.isCompleted
        )
        if (todo.description != null) {
            data["description"] = todo.description
        } else {
            data["description"] = FieldValue.delete()
        }
        if (todo.dueDate != null) {
            data["dueDate"] = todo.dueDate
        } else {
            data["dueDate"] = FieldValue.delete()
        }
        userTodosCollection().document(todo.id).update(data).await()
    }

    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit> = runCatching {
        userTodosCollection().document(todoId)
            .update("isCompleted", isCompleted).await()
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> = runCatching {
        userTodosCollection().document(todoId).delete().await()
    }
}
