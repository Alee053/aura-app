package com.programovil.aura.habit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.model.*
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.*
import com.programovil.aura.shared.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

data class HabitListUiState(
    val habits: List<HabitWithStatus> = emptyList(), val isLoading: Boolean = true,
    val error: UiText? = null, val loadError: UiText? = null
)
sealed class HabitEvent {
    data class ToggleCompletion(val habitId: String, val date: String) : HabitEvent()
    data class AddHabit(val name: String, val recurrenceType: RecurrenceType, val targetCount: Int, val color: String) : HabitEvent()
    data class UpdateHabit(val habit: Habit) : HabitEvent()
    data class DeleteHabit(val habitId: String) : HabitEvent()
}
class HabitViewModel(
    private val repository: HabitRepository,
    private val getHabitsWithStatusUseCase: GetHabitsWithStatusUseCase,
    private val addHabitUseCase: AddHabitUseCase, private val updateHabitUseCase: UpdateHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState = _uiState.asStateFlow()
    val operations = UiOperations(viewModelScope)
    private var loadJob: Job? = null
    private var latestHabits = emptyList<HabitWithStatus>()
    private val heldHabits = mutableMapOf<String, HabitWithStatus>()
    private fun publishHabits() {
        val visible = latestHabits.map { heldHabits[it.habit.id] ?: it } + heldHabits.values.filter { held -> latestHabits.none { it.habit.id == held.habit.id } }
        _uiState.update { it.copy(habits = visible) }
    }
    private fun hold(id: String) { if (id !in heldHabits) _uiState.value.habits.find { it.habit.id == id }?.let { heldHabits[id] = it } }
    private fun release(id: String, key: String, result: Result<Unit>) {
        if (operations.states.value.any { (other, op) -> other != key && other.startsWith("toggle:$id:") && op.pending }) return
        val previous = heldHabits.remove(id)
        if (result.isFailure && previous != null) latestHabits = latestHabits.filterNot { it.habit.id == id } + previous
        publishHabits()
    }
    init { retryLoad() }
    fun retryLoad() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = it.habits.isEmpty()) }
        loadJob = viewModelScope.launch {
            getHabitsWithStatusUseCase().catch { emit(Result.failure(it)) }.collect { result ->
                result.onSuccess { habits -> latestHabits = habits; publishHabits(); _uiState.update { it.copy(isLoading = false, loadError = null) } }
                    .onFailure { _uiState.update { it.copy(isLoading = false, loadError = ErrorKey.HabitLoad, error = ErrorKey.HabitLoad) } }
            }
        }
    }
    private fun run(key: String, kind: String, error: UiText, habitId: String? = null, action: suspend () -> Result<Unit>) {
        if (operations.isPending(key)) return
        habitId?.let(::hold)
        operations.launch(key, kind, error, { result ->
            habitId?.let { release(it, key, result) }
            if (result.isFailure) _uiState.update { it.copy(error = error) }
        }, action)
    }
    fun onEvent(event: HabitEvent) {
        when (event) {
            is HabitEvent.ToggleCompletion -> run("toggle:${event.habitId}:${event.date}", "toggle", ErrorKey.HabitUpdate, event.habitId) {
                toggleHabitCompletionUseCase(event.habitId, event.date)
            }
            is HabitEvent.AddHabit -> run("editor", "save", ErrorKey.HabitAdd) {
                addHabitUseCase(event.name, event.recurrenceType, event.targetCount, event.color)
            }
            is HabitEvent.UpdateHabit -> run("editor", "save", ErrorKey.HabitUpdate, event.habit.id) { updateHabitUseCase(event.habit) }
            is HabitEvent.DeleteHabit -> run("editor", "delete", ErrorKey.HabitDelete, event.habitId) { deleteHabitUseCase(event.habitId) }
        }
    }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun getTodayDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}
