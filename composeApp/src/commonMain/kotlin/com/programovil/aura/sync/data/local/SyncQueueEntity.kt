package com.programovil.aura.sync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val type: String, // "TODO" o "HABIT"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)