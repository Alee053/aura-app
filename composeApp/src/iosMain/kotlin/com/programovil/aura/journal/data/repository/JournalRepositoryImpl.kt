package com.programovil.aura.journal.domain.repository

import com.programovil.aura.journal.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createJournalRepository(): JournalRepository = IosJournalRepositoryImpl()

private class IosJournalRepositoryImpl : JournalRepository {
    override fun getEntries(): Flow<List<JournalEntry>> = flowOf(emptyList())
    override suspend fun getEntry(id: String): JournalEntry? = null
    override suspend fun addEntry(title: String, content: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)
    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)
}
