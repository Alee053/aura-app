package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository

class GetJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: String): JournalEntry? = repository.getEntry(id)
}
