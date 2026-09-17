package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.usecase.*
import com.programovil.aura.shared.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(), val isLoading: Boolean = true,
    val error: UiText? = null, val loadError: UiText? = null
)
class JournalViewModel(
    private val getEntriesUseCase: GetJournalEntriesUseCase,
    private val deleteEntryUseCase: DeleteJournalEntryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState = _uiState.asStateFlow()
    val operations = UiOperations(viewModelScope)
    private var loadJob: Job? = null
    private var latestEntries = emptyList<JournalEntry>()
    private val deletingEntries = mutableMapOf<String, JournalEntry>()
    private fun publishEntries() {
        val visible = (latestEntries + deletingEntries.values.filter { held -> latestEntries.none { it.id == held.id } }).sortedByDescending { it.createdAt }
        _uiState.update { it.copy(entries = visible) }
    }
    init { retryLoad() }
    fun retryLoad() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = it.entries.isEmpty()) }
        loadJob = viewModelScope.launch {
            getEntriesUseCase().catch { emit(Result.failure(it)) }.collect { result ->
                result.onSuccess { entries ->
                    latestEntries = entries
                    publishEntries()
                    _uiState.update { it.copy(isLoading = false, error = null, loadError = null) }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, error = ErrorKey.JournalLoad, loadError = ErrorKey.JournalLoad) }
                }
            }
        }
    }
    fun deleteEntry(entry: JournalEntry) {
        if (operations.isPending(entry.id)) return
        deletingEntries[entry.id] = entry
        operations.launch(entry.id, "delete", ErrorKey.JournalDelete, { result ->
            deletingEntries.remove(entry.id)
            if (result.isFailure) {
                latestEntries = latestEntries.filterNot { it.id == entry.id } + entry
                _uiState.update { it.copy(error = ErrorKey.JournalDelete) }
            }
            publishEntries()
        }) { deleteEntryUseCase(entry) }
    }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}
