package com.programovil.aura.habit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitListUiState(
    val todayHabits: List<HabitWithStatus> = emptyList(),
    val tomorrowHabits: List<HabitWithStatus> = emptyList(),
    val thisWeekHabits: List<HabitWithStatus> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class HabitEvent {
    data class ToggleCompletion(val habitId: String, val date: String) : HabitEvent()
    data class AddHabit(
        val name: String,
        val recurrenceType: RecurrenceType,
        val daysOfWeek: List<Int>,
        val color: String
    ) : HabitEvent()
}

class HabitViewModel(
    private val repository: HabitRepository,
    private val getHabitsGroupedByDayUseCase: GetHabitsGroupedByDayUseCase,
    private val addHabitUseCase: AddHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState: StateFlow<HabitListUiState> = _uiState

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        viewModelScope.launch {
            repository.cleanupOldCompletions(90)
        }
        loadHabits()
    }

    fun onEvent(event: HabitEvent) {
        when (event) {
            is HabitEvent.ToggleCompletion -> toggleCompletion(event.habitId, event.date)
            is HabitEvent.AddHabit -> addHabit(event.name, event.recurrenceType, event.daysOfWeek, event.color)
        }
    }

    private fun loadHabits() {
        viewModelScope.launch {
            getHabitsGroupedByDayUseCase().collect { grouped ->
                _uiState.value = HabitListUiState(
                    todayHabits = grouped[DaySection.TODAY] ?: emptyList(),
                    tomorrowHabits = grouped[DaySection.TOMORROW] ?: emptyList(),
                    thisWeekHabits = grouped[DaySection.THIS_WEEK] ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }

    private fun toggleCompletion(habitId: String, date: String) {
        viewModelScope.launch {
            toggleHabitCompletionUseCase(habitId, date)
                .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to update habit") }
        }
    }

    private fun addHabit(name: String, recurrenceType: RecurrenceType, daysOfWeek: List<Int>, color: String) {
        viewModelScope.launch {
            addHabitUseCase(name, recurrenceType, daysOfWeek, color)
                .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to add habit") }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getTodayDate(): String = LocalDate.now().format(dateFormatter)
    fun getTomorrowDate(): String = LocalDate.now().plusDays(1).format(dateFormatter)
}