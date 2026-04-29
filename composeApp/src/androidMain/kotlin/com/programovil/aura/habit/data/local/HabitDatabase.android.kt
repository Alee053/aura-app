package com.programovil.aura.habit.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun getHabitDatabaseBuilder(): RoomDatabase.Builder<HabitDatabase> {
    // We need context here. Since this is an expect/actual, we can either:
    // 1. Pass context as param (but that would complicate commonMain)
    // 2. Use a Global Context or Koin to get it.
    // Given the project uses Koin, we can use a KoinComponent or similar.
    
    val context: Context = KoinContext.applicationContext
    val dbFile = context.getDatabasePath("habit_database.db")
    return Room.databaseBuilder<HabitDatabase>(
        context = context,
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration()
}

object KoinContext : KoinComponent {
    val applicationContext: Context get() = get()
}
