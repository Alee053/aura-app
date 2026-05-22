package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.*

class GetHabitsWithStatusUseCase(private val repository: HabitRepository) {

    operator fun invoke(): Flow<Result<List<HabitWithStatus>>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return combine(
            repository.getHabits(),
            repository.getAllCompletions()
        ) { habitsResult, completionsResult ->
            habitsResult.fold(
                onSuccess = { habits ->
                    completionsResult.fold(
                        onSuccess = { completions ->
                            val result = habits.map { habit ->
                                val habitCompletions = completions.filter { it.habitId == habit.id }
                                val streak = calculateStreak(habit, habitCompletions, today)
                                val progress = currentPeriodProgress(habit, habitCompletions.map { it.completedDate }, today)
                                val last7 = computeLast7Days(habitCompletions.map { it.completedDate }, today)
                                HabitWithStatus(
                                    habit = habit,
                                    currentPeriodProgress = progress,
                                    streak = streak,
                                    last7Days = last7
                                )
                            }
                            Result.success(result)
                        },
                        onFailure = { Result.failure(it) }
                    )
                },
                onFailure = { Result.failure(it) }
            )
        }
    }

    private data class Period(val startDate: LocalDate, val endDate: LocalDate)

    private fun getPeriodContaining(date: LocalDate, type: RecurrenceType): Period {
        return when (type) {
            RecurrenceType.DAILY -> Period(date, date)
            RecurrenceType.WEEKLY -> {
                val daysSinceMonday = date.dayOfWeek.isoDayNumber - 1
                val monday = date.minus(daysSinceMonday, DateTimeUnit.DAY)
                Period(monday, monday.plus(6, DateTimeUnit.DAY))
            }
            RecurrenceType.MONTHLY -> {
                val firstDay = LocalDate(date.year, date.monthNumber, 1)
                val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                Period(firstDay, lastDay)
            }
        }
    }

    private fun previousPeriod(period: Period, type: RecurrenceType): Period {
        return when (type) {
            RecurrenceType.DAILY -> {
                val prev = period.startDate.minus(1, DateTimeUnit.DAY)
                Period(prev, prev)
            }
            RecurrenceType.WEEKLY -> {
                val prev = period.startDate.minus(7, DateTimeUnit.DAY)
                Period(prev, prev.plus(6, DateTimeUnit.DAY))
            }
            RecurrenceType.MONTHLY -> {
                val firstDay = period.startDate.minus(1, DateTimeUnit.MONTH)
                val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                Period(firstDay, lastDay)
            }
        }
    }

    private fun countCompletionsInPeriod(completedDates: Set<String>, period: Period): Int {
        return completedDates.count { dateStr ->
            val date = LocalDate.parse(dateStr)
            date >= period.startDate && date <= period.endDate
        }
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, today: LocalDate): Int {
        val completedDates = completions.map { it.completedDate }.toSet()
        val currentPeriod = getPeriodContaining(today, habit.recurrenceType)
        val currentCount = countCompletionsInPeriod(completedDates, currentPeriod)
        val currentPeriodComplete = currentPeriod.endDate < today

        // If current period is complete and doesn't meet target, streak is 0
        if (currentPeriodComplete && currentCount < habit.targetCount) {
            return 0
        }

        // Count completions from consecutive previous periods that met the target
        var streak = 0
        var period = previousPeriod(currentPeriod, habit.recurrenceType)
        var searchLimit = 100

        while (searchLimit-- > 0) {
            val count = countCompletionsInPeriod(completedDates, period)
            if (count >= habit.targetCount) {
                streak += count
                period = previousPeriod(period, habit.recurrenceType)
            } else {
                break
            }
        }

        // Add current period completions
        streak += currentCount

        return streak
    }

    private fun currentPeriodProgress(habit: Habit, completions: List<String>, today: LocalDate): Pair<Int, Int> {
        val period = getPeriodContaining(today, habit.recurrenceType)
        val count = countCompletionsInPeriod(completions.toSet(), period)
        return Pair(count, habit.targetCount)
    }

    private fun computeLast7Days(completions: List<String>, today: LocalDate): List<DayCompletion> {
        val completedDates = completions.toSet()
        return (6 downTo 0).map { daysAgo ->
            val date = today.minus(daysAgo, DateTimeUnit.DAY)
            DayCompletion(
                date = date.toString(),
                isCompleted = completedDates.contains(date.toString()),
                isScheduled = true
            )
        }
    }
}