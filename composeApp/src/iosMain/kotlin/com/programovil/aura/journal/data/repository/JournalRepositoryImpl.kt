package com.programovil.aura.journal.data.repository

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createJournalRepository(): JournalRepository = IosJournalRepositoryImpl()

private class IosJournalRepositoryImpl : JournalRepository {
    override fun getEntries(): Flow<Result<List<JournalEntry>>> = flowOf(Result.success(emptyList()))
    override suspend fun getEntry(id: String): JournalEntry? = null
    override suspend fun addEntry(title: String, content: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)
    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> = Result.success(Unit)
}
