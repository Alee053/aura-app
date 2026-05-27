package com.programovil.aura.journal.data.repository

import com.programovil.aura.journal.data.dao.JournalDao
import com.programovil.aura.journal.data.entity.JournalEntity
import com.programovil.aura.journal.data.mapper.toDomain
import com.programovil.aura.journal.data.mapper.toEntity
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class JournalRepositoryImpl(
    private val dao: JournalDao
) : JournalRepository {

    override fun getEntries(): Flow<List<JournalEntry>> =
        dao.getAllEntries().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEntry(id: String): JournalEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun addEntry(title: String, content: String): Result<Unit> =
        runCatching {
            val now = Clock.System.now().toEpochMilliseconds()
            val entity = JournalEntity(
                id = generateJournalId(),
                title = title,
                content = content,
                createdAt = now,
                updatedAt = now
            )
            dao.insert(entity)
        }

    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> =
        runCatching {
            dao.update(entry.copy(updatedAt = Clock.System.now().toEpochMilliseconds()).toEntity())
        }

    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> =
        runCatching { dao.delete(entry.toEntity()) }
}

private fun generateJournalId(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..16).map { chars.random() }.joinToString("")
}
