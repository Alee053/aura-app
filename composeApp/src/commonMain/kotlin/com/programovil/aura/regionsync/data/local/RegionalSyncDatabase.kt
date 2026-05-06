package com.programovil.aura.regionsync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RegionDataEntity::class], version = 1, exportSchema = false)
abstract class RegionalSyncDatabase : RoomDatabase() {
    abstract fun regionDataDao(): RegionDataDao
}
