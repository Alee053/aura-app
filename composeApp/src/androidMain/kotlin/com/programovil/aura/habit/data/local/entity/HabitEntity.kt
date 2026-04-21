package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val recurrenceType: String, // "DAILY" or "WEEKLY"
    val daysOfWeek: String, // comma-separated, e.g., "1,3,5" for Mon,Wed,Fri. Empty for DAILY.
    val color: String,
    val createdAt: Long
)