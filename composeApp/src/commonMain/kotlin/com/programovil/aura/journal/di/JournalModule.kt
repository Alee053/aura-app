package com.programovil.aura.journal.di

import com.programovil.aura.journal.data.repository.createJournalRepository
import com.programovil.aura.journal.domain.repository.JournalRepository
import com.programovil.aura.journal.domain.usecase.AddJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.DeleteJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntriesUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.UpdateJournalEntryUseCase
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val journalModule = module {
    single<JournalRepository> { createJournalRepository() }

    factoryOf(::GetJournalEntriesUseCase)
    factoryOf(::GetJournalEntryUseCase)
    factoryOf(::AddJournalEntryUseCase)
    factoryOf(::UpdateJournalEntryUseCase)
    factoryOf(::DeleteJournalEntryUseCase)

    viewModelOf(::JournalViewModel)
    viewModel { (entryId: String?) ->
        JournalDetailViewModel(entryId, get(), get(), get())
    }
}
