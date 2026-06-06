package com.programovil.aura.journal.data.entity

data class JournalEntity(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
