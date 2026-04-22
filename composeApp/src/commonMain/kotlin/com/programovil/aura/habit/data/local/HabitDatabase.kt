package com.programovil.aura.habit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.programovil.aura.habit.data.local.entity.HabitCompletionEntity
import com.programovil.aura.habit.data.local.entity.HabitEntity
import com.programovil.aura.sync.data.local.dao.SyncQueueDao
import com.programovil.aura.sync.data.local.entity.SyncQueueEntity

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class, SyncQueueEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun syncQueueDao(): SyncQueueDao
}

expect fun getHabitDatabaseBuilder(): RoomDatabase.Builder<HabitDatabase>

fun getHabitDatabase(builder: RoomDatabase.Builder<HabitDatabase>): HabitDatabase {
    return builder
        .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
        .setQueryCoroutineContext(kotlinx.coroutines.Dispatchers.IO)
        .build()
}
