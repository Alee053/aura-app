package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class ProgressUiState(
    val streak: Int = 0,
    val totalCompleted: Int = 0,
    val activeDays: Int = 0,
    val isLoading: Boolean = false
)

class ProgressViewModel(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                habitRepository.getHabits(),
                habitRepository.getAllCompletions()
            ) { habitsResult, completionsResult ->
                habitsResult.fold(
                    onSuccess = { habits ->
                        completionsResult.fold(
                            onSuccess = { completions ->
                                val totalCompleted = completions.size
                                val activeDays = completions.map { it.completedDate }.distinct().size
                                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                val maxStreak = habits.maxOfOrNull { habit ->
                                    calculateStreak(habit, completions, today)
                                } ?: 0
                                Result.success(
                                    ProgressUiState(
                                        streak = maxStreak,
                                        totalCompleted = totalCompleted,
                                        activeDays = activeDays,
                                        isLoading = false
                                    )
                                )
                            },
                            onFailure = { Result.failure(it) }
                        )
                    },
                    onFailure = { Result.failure(it) }
                )
            }.collect { result ->
                result.onSuccess { state ->
                    _uiState.value = state
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, fromDate: kotlinx.datetime.LocalDate): Int {
        var streak = 0
        var currentDate = fromDate
        val completedDates = completions.filter { it.habitId == habit.id }.map { it.completedDate }.toSet()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        repeat(90) {
            if (habit.recurrenceType == com.programovil.aura.habit.domain.model.RecurrenceType.DAILY ||
                habit.daysOfWeek.contains(currentDate.dayOfWeek.ordinal + 1)
            ) {
                val dateStr = currentDate.toString()
                if (completedDates.contains(dateStr)) {
                    streak++
                } else if (currentDate <= today) {
                    return streak
                }
            }
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}