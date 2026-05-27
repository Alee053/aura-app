package com.programovil.aura.journal.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun getJournalDatabaseBuilder(): RoomDatabase.Builder<JournalDatabase> {
    val context: Context = object : KoinComponent {
        val ctx: Context = get()
    }.ctx
    val dbFile = context.applicationContext.getDatabasePath("journal.db")
    return Room.databaseBuilder<JournalDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}