package com.programovil.aura.sync.data.repository

import com.programovil.aura.habit.data.local.HabitDatabase

class StubFirestoreSyncService : IFirestoreSyncService {
    override suspend fun syncHabit(habitData: Map<String, Any>, habitId: String, action: String): Boolean = true
    override suspend fun syncTodo(todoData: Map<String, Any>, todoId: String?, action: String): Boolean = true
    override suspend fun syncCompletion(completionData: Map<String, Any>, completionId: String, action: String): Boolean = true
    override suspend fun enqueueSync(entityType: String, entityId: String, action: String, data: String): Boolean = true
}

actual fun createFirestoreSyncService(database: HabitDatabase): IFirestoreSyncService = StubFirestoreSyncService()