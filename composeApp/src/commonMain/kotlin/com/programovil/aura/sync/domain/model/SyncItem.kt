package com.programovil.aura.sync.domain.model

data class SyncItem(
    val id: String,
    val payloadJson: String,
    val createdAt: Long
)
