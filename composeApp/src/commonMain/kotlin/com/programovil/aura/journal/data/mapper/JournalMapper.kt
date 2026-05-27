package com.programovil.aura.journal.data.mapper

import com.programovil.aura.journal.data.entity.JournalEntity
import com.programovil.aura.journal.domain.model.JournalEntry

fun JournalEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun JournalEntry.toEntity(): JournalEntity = JournalEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
