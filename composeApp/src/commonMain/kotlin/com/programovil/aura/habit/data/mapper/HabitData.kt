package com.programovil.aura.habit.data.mapper

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType

data class HabitData(
    val id: String,
    val name: String,
    val recurrenceType: String,
    val daysOfWeek: List<Int> = emptyList(),
    val color: String,
    val createdAt: Long? = null
)

data class HabitCompletionData(
    val id: String,
    val habitId: String,
    val completedDate: String,
    val completedAt: Long? = null
)

fun HabitData.toDomain(): Habit = Habit(
    id = id,
    name = name,
    recurrenceType = RecurrenceType.valueOf(recurrenceType),
    daysOfWeek = daysOfWeek,
    color = color,
    createdAt = createdAt ?: 0L
)

fun Habit.toData(): HabitData = HabitData(
    id = id,
    name = name,
    recurrenceType = recurrenceType.name,
    daysOfWeek = daysOfWeek,
    color = color,
    createdAt = createdAt
)

fun HabitCompletionData.toDomain(): HabitCompletion = HabitCompletion(
    id = id,
    habitId = habitId,
    completedDate = completedDate,
    completedAt = completedAt ?: 0L
)

fun HabitCompletion.toData(): HabitCompletionData = HabitCompletionData(
    id = id,
    habitId = habitId,
    completedDate = completedDate,
    completedAt = completedAt
)