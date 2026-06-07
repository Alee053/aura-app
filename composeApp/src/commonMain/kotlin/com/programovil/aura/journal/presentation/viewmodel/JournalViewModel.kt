package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.usecase.DeleteJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class JournalViewModel(
    private val getEntriesUseCase: GetJournalEntriesUseCase,
    private val deleteEntryUseCase: DeleteJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            getEntriesUseCase().collect { result ->
                result.onSuccess { entries ->
                    _uiState.value = _uiState.value.copy(
                        entries = entries,
                        isLoading = false,
                        error = null
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load journal entries"
                    )
                }
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            deleteEntryUseCase(entry).onFailure {
                _uiState.value = _uiState.value.copy(error = "Failed to delete entry")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
