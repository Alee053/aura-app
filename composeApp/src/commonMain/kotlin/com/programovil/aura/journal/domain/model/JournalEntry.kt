package com.programovil.aura.journal.domain.model

data class JournalEntry(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
