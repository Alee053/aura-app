package com.programovil.aura.habit.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual fun getHabitDatabaseBuilder(): RoomDatabase.Builder<HabitDatabase> {
    val dbFile = NSHomeDirectory() + "/habit_database.db"
    return Room.databaseBuilder<HabitDatabase>(
        name = dbFile,
        factory = { HabitDatabase::class.instantiateImpl() }
    )
}
