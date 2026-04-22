package com.programovil.aura.sync.data.repository

import com.programovil.aura.habit.data.local.HabitDatabase

expect fun createFirestoreSyncService(database: HabitDatabase): IFirestoreSyncService

interface IFirestoreSyncService {
    suspend fun syncHabit(habitData: Map<String, Any>, habitId: String, action: String): Boolean
    suspend fun syncTodo(todoData: Map<String, Any>, todoId: String?, action: String): Boolean
    suspend fun syncCompletion(completionData: Map<String, Any>, completionId: String, action: String): Boolean
    suspend fun enqueueSync(entityType: String, entityId: String, action: String, data: String): Boolean
}