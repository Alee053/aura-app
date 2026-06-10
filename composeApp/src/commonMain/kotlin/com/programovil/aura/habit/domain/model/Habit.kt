package com.programovil.aura.habit.domain.model

import kotlin.time.Clock

data class Habit(
    val id: String,
    val name: String,
    val recurrenceType: RecurrenceType,
    val targetCount: Int,
    val color: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)
