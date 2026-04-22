package com.programovil.aura.sync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val data: String,
    val createdAt: Long,
    val pending: Boolean = true,
    val retryCount: Int = 0
)

enum class EntityType {
    HABIT, TODO, COMPLETION
}

enum class SyncAction {
    CREATE, UPDATE, DELETE
}