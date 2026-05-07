package com.programovil.aura.regionsync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RegionDataEntity::class], version = 1, exportSchema = false)
abstract class RegionalSyncDatabase : RoomDatabase() {
    abstract fun regionDataDao(): RegionDataDao
}

expect fun getRegionalSyncDatabaseBuilder(): RoomDatabase.Builder<RegionalSyncDatabase>

fun getRegionalSyncDatabase(builder: RoomDatabase.Builder<RegionalSyncDatabase>): RegionalSyncDatabase {
    return builder
        .fallbackToDestructiveMigration(true)
        .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
        .setQueryCoroutineContext(kotlinx.coroutines.Dispatchers.IO)
        .build()
}

expect object RegionalSyncDatabaseCompanion {
    fun create(context: android.content.Context): RegionalSyncDatabase
}
