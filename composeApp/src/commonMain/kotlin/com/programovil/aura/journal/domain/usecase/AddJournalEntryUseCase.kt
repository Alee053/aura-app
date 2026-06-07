package com.programovil.aura.journal.domain.usecase

import com.programovil.aura.journal.domain.repository.JournalRepository

class AddJournalEntryUseCase(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(title: String, content: String): Result<Unit> =
        repository.addEntry(title, content)
}
