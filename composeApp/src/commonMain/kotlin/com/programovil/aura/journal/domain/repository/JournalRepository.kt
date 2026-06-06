package com.programovil.aura.journal.domain.repository

import com.programovil.aura.journal.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getEntries(): Flow<List<JournalEntry>>
    suspend fun getEntry(id: String): JournalEntry?
    suspend fun addEntry(title: String, content: String): Result<Unit>
    suspend fun updateEntry(entry: JournalEntry): Result<Unit>
    suspend fun deleteEntry(entry: JournalEntry): Result<Unit>
}

expect fun createJournalRepository(): JournalRepository
