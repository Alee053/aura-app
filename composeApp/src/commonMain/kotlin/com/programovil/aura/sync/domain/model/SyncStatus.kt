package com.programovil.aura.sync.domain.model

data class SyncStatus(
    val pendingCount: Int,
    val isSyncing: Boolean,
    val lastSyncTime: Long?
)