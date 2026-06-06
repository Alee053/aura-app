package com.programovil.aura.journal.domain.repository

import com.programovil.aura.journal.domain.model.JournalEntry
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
interface JournalRepository {
    fun getEntries(): Flow<Result<List<JournalEntry>>>
    suspend fun getEntry(id: String): JournalEntry?
    suspend fun addEntry(title: String, content: String): Result<Unit>
    suspend fun updateEntry(entry: JournalEntry): Result<Unit>
    suspend fun deleteEntry(entry: JournalEntry): Result<Unit>
}
