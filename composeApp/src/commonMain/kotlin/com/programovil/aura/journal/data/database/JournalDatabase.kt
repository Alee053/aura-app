package com.programovil.aura.journal.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programovil.aura.journal.data.dao.JournalDao
import com.programovil.aura.journal.data.entity.JournalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [JournalEntity::class], version = 1)
@ConstructedBy(JournalDatabaseConstructor::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}

@Suppress("KotlinNoActualForExpect")
expect object JournalDatabaseConstructor : RoomDatabaseConstructor<JournalDatabase> {
    override fun initialize(): JournalDatabase
}

expect fun getJournalDatabaseBuilder(): RoomDatabase.Builder<JournalDatabase>

fun getJournalDatabase(builder: RoomDatabase.Builder<JournalDatabase>): JournalDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}