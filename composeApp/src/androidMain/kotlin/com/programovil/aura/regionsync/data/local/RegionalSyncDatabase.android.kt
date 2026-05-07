package com.programovil.aura.regionsync.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun getRegionalSyncDatabaseBuilder(): RoomDatabase.Builder<RegionalSyncDatabase> {
    val context: Context = KoinContext.applicationContext
    val dbFile = context.getDatabasePath("regional_sync_database.db")
    return Room.databaseBuilder<RegionalSyncDatabase>(
        context = context,
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration()
}

object KoinContext : KoinComponent {
    val applicationContext: Context get() = get()
}

actual object RegionalSyncDatabaseCompanion {
    actual fun create(context: Context): RegionalSyncDatabase {
        val dbFile = context.getDatabasePath("regional_sync_database.db")
        return Room.databaseBuilder<RegionalSyncDatabase>(
            context = context,
            name = dbFile.absolutePath
        ).fallbackToDestructiveMigration().build()
    }
}
