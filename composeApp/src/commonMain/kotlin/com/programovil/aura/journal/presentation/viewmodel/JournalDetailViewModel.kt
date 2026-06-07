package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.usecase.AddJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.GetJournalEntryUseCase
import com.programovil.aura.journal.domain.usecase.UpdateJournalEntryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalDetailUiState(
    val entry: JournalEntry? = null,
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class JournalDetailViewModel(
    private val entryId: String?,
    private val getEntryUseCase: GetJournalEntryUseCase,
    private val addEntryUseCase: AddJournalEntryUseCase,
    private val updateEntryUseCase: UpdateJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalDetailUiState())
    val uiState: StateFlow<JournalDetailUiState> = _uiState.asStateFlow()

    init {
        if (entryId != null) {
            loadEntry(entryId)
        }
    }

    private fun loadEntry(id: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val entry = getEntryUseCase(id)
            if (entry != null) {
                _uiState.value = JournalDetailUiState(
                    entry = entry,
                    title = entry.title,
                    content = entry.content,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Entry not found"
                )
            }
        }
    }

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun updateContent(value: String) {
        _uiState.value = _uiState.value.copy(content = value)
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val result = if (state.entry != null) {
                updateEntryUseCase(
                    state.entry.copy(
                        title = state.title.trim(),
                        content = state.content.trim()
                    )
                )
            } else {
                addEntryUseCase(state.title.trim(), state.content.trim())
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaved = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Failed to save entry")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}