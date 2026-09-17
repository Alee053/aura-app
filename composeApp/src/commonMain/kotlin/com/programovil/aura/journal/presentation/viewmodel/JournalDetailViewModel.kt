package com.programovil.aura.journal.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.usecase.*
import com.programovil.aura.shared.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class JournalDetailUiState(
    val entry: JournalEntry? = null, val title: String = "", val content: String = "",
    val isLoading: Boolean = false, val isSaved: Boolean = false,
    val error: UiText? = null, val loadError: UiText? = null,
    val isNew: Boolean = true, val dirty: Boolean = false
)
class JournalDetailViewModel(
    private val entryId: String?,
    private val getEntryUseCase: GetJournalEntryUseCase,
    private val addEntryUseCase: AddJournalEntryUseCase,
    private val updateEntryUseCase: UpdateJournalEntryUseCase,
    private val deleteEntryUseCase: DeleteJournalEntryUseCase? = null,
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    private val draftKey = "draft:${entryId ?: "new"}:"
    private val _uiState = MutableStateFlow(JournalDetailUiState(
        title = savedState[draftKey + "title"] ?: "",
        content = savedState[draftKey + "content"] ?: "",
        isNew = entryId == null,
        dirty = savedState[draftKey + "dirty"] ?: false
    ))
    val uiState = _uiState.asStateFlow()
    val operations = UiOperations(viewModelScope)
    private var loadJob: Job? = null
    init { if (entryId != null) retryLoad() }
    fun retryLoad() {
        val id = entryId ?: return
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            try {
                val entry = getEntryUseCase(id)
                if (entry == null) _uiState.update {
                    it.copy(isLoading = false, loadError = ErrorKey.JournalNotFound, error = ErrorKey.JournalNotFound)
                } else _uiState.update {
                    it.copy(entry = entry,
                        title = if (it.dirty) it.title else entry.title,
                        content = if (it.dirty) it.content else entry.content,
                        isLoading = false, loadError = null, error = null)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = ErrorKey.JournalLoad, error = ErrorKey.JournalLoad) }
            }
        }
    }
    private fun saveDraft(title: String, content: String) {
        val entry = _uiState.value.entry
        val dirty = title != (entry?.title ?: "") || content != (entry?.content ?: "")
        savedState[draftKey + "title"] = title
        savedState[draftKey + "content"] = content
        savedState[draftKey + "dirty"] = dirty
        _uiState.update { it.copy(title = title, content = content, dirty = dirty) }
    }
    fun updateTitle(value: String) { if (!operations.isPending("editor")) saveDraft(value, _uiState.value.content) }
    fun updateContent(value: String) { if (!operations.isPending("editor")) saveDraft(_uiState.value.title, value) }
    fun saveEntry() {
        val state = _uiState.value
        if (state.title.isBlank() || state.isLoading || state.loadError != null || state.isSaved) return
        operations.launch("editor", "save", ErrorKey.JournalSave, { result ->
            result.onSuccess { _uiState.update { it.copy(isSaved = true, dirty = false) }; clearDraft() }
                .onFailure { _uiState.update { it.copy(error = ErrorKey.JournalSave) } }
        }) {
            if (state.entry != null) updateEntryUseCase(state.entry.copy(title = state.title.trim(), content = state.content.trim()))
            else addEntryUseCase(state.title.trim(), state.content.trim())
        }
    }
    fun deleteEntry() {
        val entry = _uiState.value.entry ?: return
        val delete = deleteEntryUseCase ?: return
        operations.launch("editor", "delete", ErrorKey.JournalDelete, { result ->
            if (result.isSuccess) { clearDraft(); _uiState.update { it.copy(isSaved = true, dirty = false) } }
            else _uiState.update { it.copy(error = ErrorKey.JournalDelete) }
        }) { delete(entry) }
    }
    fun clearDraft() {
        savedState.remove<String>(draftKey + "title")
        savedState.remove<String>(draftKey + "content")
        savedState.remove<Boolean>(draftKey + "dirty")
    }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}
