package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.*

class GetHabitsGroupedByDayUseCase(private val repository: HabitRepository) {

    operator fun invoke(): Flow<Result<Map<DaySection, List<HabitWithStatus>>>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        return combine(
            repository.getHabits(),
            repository.getAllCompletions()
        ) { habitsResult, completionsResult ->
            habitsResult.fold(
                onSuccess = { habits ->
                    completionsResult.fold(
                        onSuccess = { completions ->
                            Result.success(buildMap {
                                put(DaySection.TODAY, groupHabitsForDate(habits, completions, today))
                                put(DaySection.TOMORROW, groupHabitsForDate(habits, completions, today.plus(1, DateTimeUnit.DAY)))
                                put(DaySection.THIS_WEEK, buildThisWeekHabits(habits, completions, today))
                            })
                        },
                        onFailure = { Result.failure(it) }
                    )
                },
                onFailure = { Result.failure(it) }
            )
        }
    }

    private fun groupHabitsForDate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        date: LocalDate
    ): List<HabitWithStatus> {
        val dayOfWeek = date.dayOfWeek.isoDayNumber // 1=Monday, 7=Sunday
        val dateStr = date.toString()

        return habits
            .filter { it.isScheduledFor(dayOfWeek) }
            .map { habit ->
                val isDone = completions.any { it.habitId == habit.id && it.completedDate == dateStr }
                val streak = calculateStreak(habit, completions, date)
                HabitWithStatus(
                    habit = habit,
                    isDone = isDone,
                    isMissed = !isDone && date < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    streak = streak,
                    targetDate = dateStr
                )
            }
    }

    private fun buildThisWeekHabits(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate
    ): List<HabitWithStatus> {
        val result = mutableListOf<HabitWithStatus>()
        // Show next 7 days starting from day after tomorrow
        for (i in 2..7) {
            val date = today.plus(i, DateTimeUnit.DAY)
            result.addAll(groupHabitsForDate(habits, completions, date))
        }
        return result
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, fromDate: LocalDate): Int {
        var streak = 0
        var currentDate = fromDate
        val completedDates = completions.filter { it.habitId == habit.id }.map { it.completedDate }.toSet()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Look back up to 90 days
        repeat(90) {
            if (habit.isScheduledFor(currentDate.dayOfWeek.isoDayNumber)) {
                val dateStr = currentDate.toString()
                if (completedDates.contains(dateStr)) {
                    streak++
                } else if (currentDate <= today) {
                    return streak // streak broken
                }
            }
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun Habit.isScheduledFor(dayOfWeek: Int): Boolean {
        return recurrenceType == RecurrenceType.DAILY || daysOfWeek.contains(dayOfWeek)
    }
}