package com.programovil.aura.habit.data.mapper

import com.programovil.aura.habit.data.local.entity.HabitCompletionEntity
import com.programovil.aura.habit.data.local.entity.HabitEntity
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType

object HabitMapper {
    fun HabitEntity.toDomain(): Habit = Habit(
        id = id,
        name = name,
        recurrenceType = RecurrenceType.valueOf(recurrenceType),
        daysOfWeek = if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map { it.toInt() },
        color = color,
        createdAt = createdAt,
        isSynced = isSynced
    )

    fun Habit.toEntity(): HabitEntity = HabitEntity(
        id = id,
        name = name,
        recurrenceType = recurrenceType.name,
        daysOfWeek = daysOfWeek.joinToString(","),
        color = color,
        createdAt = createdAt,
        isSynced = isSynced
    )

    fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
        id = id,
        habitId = habitId,
        completedDate = completedDate,
        completedAt = completedAt
    )

    fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
        id = id,
        habitId = habitId,
        completedDate = completedDate,
        completedAt = completedAt
    )
}