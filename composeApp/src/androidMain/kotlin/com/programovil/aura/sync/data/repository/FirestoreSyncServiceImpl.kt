package com.programovil.aura.sync.data.repository

import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.tasks.await

class FirestoreSyncService : IFirestoreSyncService {
    private val firestore = FirebaseConfig.firestore
    private val userId: String? get() = FirebaseConfig.auth.currentUser?.uid

    override suspend fun syncHabit(habitData: Map<String, Any>, habitId: String, action: String): Boolean {
        return try {
            val uid = userId ?: return false
            val collection = firestore.collection("users").document(uid).collection("habits")

            when (action) {
                "CREATE" -> collection.document(habitId).set(habitData).await()
                "UPDATE" -> collection.document(habitId).update(habitData).await()
                "DELETE" -> collection.document(habitId).delete().await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncTodo(todoData: Map<String, Any>, todoId: String?, action: String): Boolean {
        return try {
            val uid = userId ?: return false
            val collection = firestore.collection("users").document(uid).collection("todos")

            when (action) {
                "CREATE" -> {
                    if (todoId != null) {
                        collection.document(todoId).set(todoData).await()
                    } else {
                        collection.add(todoData).await()
                    }
                }
                "UPDATE" -> {
                    if (todoId != null) {
                        collection.document(todoId).update(todoData).await()
                    }
                }
                "DELETE" -> {
                    if (todoId != null) {
                        collection.document(todoId).delete().await()
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncCompletion(completionData: Map<String, Any>, completionId: String, action: String): Boolean {
        return try {
            val uid = userId ?: return false
            val collection = firestore.collection("users").document(uid).collection("completions")

            when (action) {
                "CREATE" -> collection.document(completionId).set(completionData).await()
                "DELETE" -> collection.document(completionId).delete().await()
                else -> false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

actual fun createFirestoreSyncService(): IFirestoreSyncService = FirestoreSyncService()