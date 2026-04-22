package com.programovil.aura.sync.data.repository

class StubFirestoreSyncService : IFirestoreSyncService {
    override suspend fun syncHabit(habitData: Map<String, Any>, habitId: String, action: String): Boolean = true
    override suspend fun syncTodo(todoData: Map<String, Any>, todoId: String?, action: String): Boolean = true
    override suspend fun syncCompletion(completionData: Map<String, Any>, completionId: String, action: String): Boolean = true
}

actual fun createFirestoreSyncService(): IFirestoreSyncService = StubFirestoreSyncService()