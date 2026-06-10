@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.programovil.aura.habit.domain.model

import kotlinx.datetime.Clock

data class HabitCompletion(
    val id: String,
    val habitId: String,
    val completedDate: String, // YYYY-MM-DD format
    val completedAt: Long = Clock.System.now().toEpochMilliseconds()
)
