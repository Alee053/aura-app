package com.programovil.aura.journal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.programovil.aura.journal.data.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntity?

    @Insert
    suspend fun insert(entry: JournalEntity)

    @Update
    suspend fun update(entry: JournalEntity)

    @Delete
    suspend fun delete(entry: JournalEntity)
}