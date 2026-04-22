package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val recurrenceType: String,
    val daysOfWeek: String,
    val color: String,
    val createdAt: Long
)
