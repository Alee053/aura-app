package com.programovil.aura.habit.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun getHabitDatabaseBuilder(): RoomDatabase.Builder<HabitDatabase> {
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
