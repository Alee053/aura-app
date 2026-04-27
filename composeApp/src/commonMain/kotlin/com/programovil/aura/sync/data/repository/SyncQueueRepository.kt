package com.programovil.aura.sync.data.repository

import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.sync.data.local.entity.EntityType
import com.programovil.aura.sync.data.local.entity.SyncAction
import com.programovil.aura.sync.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

class SyncQueueRepository(
    private val database: HabitDatabase
) {
    private val dao = database.syncQueueDao()

    fun getPendingItems(): Flow<List<SyncQueueEntity>> = dao.getPendingItems()

    suspend fun getPendingItemsSync(): List<SyncQueueEntity> = dao.getPendingItemsSync()

    suspend fun enqueue(
        entityType: EntityType,
        entityId: String,
        action: SyncAction,
        data: String
    ) {
        val item = SyncQueueEntity(
            id = generateId(),
            entityType = entityType.name,
            entityId = entityId,
            action = action.name,
            data = data,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            pending = true,
            retryCount = 0
        )
        dao.insert(item)
    }

    suspend fun markCompleted(id: String) = dao.markCompleted(id)

    suspend fun incrementRetryCount(id: String) = dao.incrementRetryCount(id)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun getPendingCount(): Int = dao.getPendingCount()

    private fun generateId(): String {
        return "sync-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
    }
}