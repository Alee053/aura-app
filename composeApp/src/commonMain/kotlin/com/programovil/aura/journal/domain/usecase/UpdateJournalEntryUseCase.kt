package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository

class UpdateJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Result<Unit> =
        repository.updateEntry(entry)
}
