package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class GetHabitsGroupedByDayUseCase(private val repository: HabitRepository) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    operator fun invoke(): Flow<Map<DaySection, List<HabitWithStatus>>> {
        val today = LocalDate.now()
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        return combine(
            repository.getHabits(),
            repository.getAllCompletions()
        ) { habits, completions ->
            buildMap {
                put(DaySection.TODAY, groupHabitsForDate(habits, completions, today))
                put(DaySection.TOMORROW, groupHabitsForDate(habits, completions, today.plusDays(1)))
                put(DaySection.THIS_WEEK, buildThisWeekHabits(habits, completions, today, endOfWeek))
            }
        }
    }

    private fun groupHabitsForDate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        date: LocalDate
    ): List<HabitWithStatus> {
        val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday
        val dateStr = date.format(dateFormatter)

        return habits
            .filter { it.isScheduledFor(dayOfWeek) }
            .map { habit ->
                val isDone = completions.any { it.habitId == habit.id && it.completedDate == dateStr }
                val streak = calculateStreak(habit, completions, date)
                HabitWithStatus(
                    habit = habit,
                    isDone = isDone,
                    isMissed = !isDone && date.isBefore(LocalDate.now()),
                    streak = streak,
                    targetDate = dateStr
                )
            }
    }

    private fun buildThisWeekHabits(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate,
        endOfWeek: LocalDate
    ): List<HabitWithStatus> {
        val result = mutableListOf<HabitWithStatus>()
        var date = today.plusDays(2) // Start from day after tomorrow
        while (!date.isAfter(endOfWeek)) {
            result.addAll(groupHabitsForDate(habits, completions, date))
            date = date.plusDays(1)
        }
        return result
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, fromDate: LocalDate): Int {
        var streak = 0
        var currentDate = fromDate
        val completedDates = completions.filter { it.habitId == habit.id }.map { it.completedDate }.toSet()

        // Look back up to 90 days
        repeat(90) {
            if (habit.isScheduledFor(currentDate.dayOfWeek.value)) {
                val dateStr = currentDate.format(dateFormatter)
                if (completedDates.contains(dateStr)) {
                    streak++
                } else if (currentDate.isBefore(LocalDate.now()) || currentDate.isEqual(LocalDate.now())) {
                    return streak // streak broken
                }
            }
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }

    private fun Habit.isScheduledFor(dayOfWeek: Int): Boolean {
        return recurrenceType == RecurrenceType.DAILY || daysOfWeek.contains(dayOfWeek)
    }
}
