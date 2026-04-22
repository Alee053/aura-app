package com.programovil.aura.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.programovil.aura.sync.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE pending = 1 ORDER BY createdAt ASC")
    fun getPendingItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE pending = 1 ORDER BY createdAt ASC")
    suspend fun getPendingItemsSync(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET pending = 0 WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: String)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_queue WHERE pending = 0")
    suspend fun deleteCompleted()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE pending = 1")
    suspend fun getPendingCount(): Int
}