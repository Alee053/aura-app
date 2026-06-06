package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow

class GetJournalEntriesUseCase(
    private val repository: JournalRepository
) {
    operator fun invoke(): Flow<Result<List<JournalEntry>>> = repository.getEntries()
}
