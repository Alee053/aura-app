package com.programovil.aura.sync.data.repository

expect fun createFirestoreSyncService(): IFirestoreSyncService

interface IFirestoreSyncService {
    suspend fun syncHabit(habitData: Map<String, Any>, habitId: String, action: String): Boolean
    suspend fun syncTodo(todoData: Map<String, Any>, todoId: String?, action: String): Boolean
    suspend fun syncCompletion(completionData: Map<String, Any>, completionId: String, action: String): Boolean
}