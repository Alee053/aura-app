package com.programovil.aura.habit.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.programovil.aura.habit.`data`.local.entity.HabitCompletionEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HabitCompletionDao_Impl(
  __db: RoomDatabase,
) : HabitCompletionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfHabitCompletionEntity: EntityInsertAdapter<HabitCompletionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfHabitCompletionEntity = object : EntityInsertAdapter<HabitCompletionEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `habit_completions` (`id`,`habitId`,`completedDate`,`completedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: HabitCompletionEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.habitId)
        statement.bindText(3, entity.completedDate)
        statement.bindLong(4, entity.completedAt)
      }
    }
  }

  public override suspend fun insertCompletion(completion: HabitCompletionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfHabitCompletionEntity.insert(_connection, completion)
  }

  public override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>> {
    val _sql: String = "SELECT * FROM habit_completions WHERE habitId = ? ORDER BY completedDate DESC"
    return createFlow(__db, false, arrayOf("habit_completions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, habitId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHabitId: Int = getColumnIndexOrThrow(_stmt, "habitId")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completedDate")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: MutableList<HabitCompletionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitCompletionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpHabitId: String
          _tmpHabitId = _stmt.getText(_columnIndexOfHabitId)
          val _tmpCompletedDate: String
          _tmpCompletedDate = _stmt.getText(_columnIndexOfCompletedDate)
          val _tmpCompletedAt: Long
          _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          _item = HabitCompletionEntity(_tmpId,_tmpHabitId,_tmpCompletedDate,_tmpCompletedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>> {
    val _sql: String = "SELECT * FROM habit_completions WHERE completedDate = ?"
    return createFlow(__db, false, arrayOf("habit_completions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHabitId: Int = getColumnIndexOrThrow(_stmt, "habitId")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completedDate")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: MutableList<HabitCompletionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitCompletionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpHabitId: String
          _tmpHabitId = _stmt.getText(_columnIndexOfHabitId)
          val _tmpCompletedDate: String
          _tmpCompletedDate = _stmt.getText(_columnIndexOfCompletedDate)
          val _tmpCompletedAt: Long
          _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          _item = HabitCompletionEntity(_tmpId,_tmpHabitId,_tmpCompletedDate,_tmpCompletedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCompletionsForDateSync(date: String): List<HabitCompletionEntity> {
    val _sql: String = "SELECT * FROM habit_completions WHERE completedDate = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHabitId: Int = getColumnIndexOrThrow(_stmt, "habitId")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completedDate")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: MutableList<HabitCompletionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitCompletionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpHabitId: String
          _tmpHabitId = _stmt.getText(_columnIndexOfHabitId)
          val _tmpCompletedDate: String
          _tmpCompletedDate = _stmt.getText(_columnIndexOfCompletedDate)
          val _tmpCompletedAt: Long
          _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          _item = HabitCompletionEntity(_tmpId,_tmpHabitId,_tmpCompletedDate,_tmpCompletedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllCompletions(): Flow<List<HabitCompletionEntity>> {
    val _sql: String = "SELECT * FROM habit_completions"
    return createFlow(__db, false, arrayOf("habit_completions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHabitId: Int = getColumnIndexOrThrow(_stmt, "habitId")
        val _columnIndexOfCompletedDate: Int = getColumnIndexOrThrow(_stmt, "completedDate")
        val _columnIndexOfCompletedAt: Int = getColumnIndexOrThrow(_stmt, "completedAt")
        val _result: MutableList<HabitCompletionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitCompletionEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpHabitId: String
          _tmpHabitId = _stmt.getText(_columnIndexOfHabitId)
          val _tmpCompletedDate: String
          _tmpCompletedDate = _stmt.getText(_columnIndexOfCompletedDate)
          val _tmpCompletedAt: Long
          _tmpCompletedAt = _stmt.getLong(_columnIndexOfCompletedAt)
          _item = HabitCompletionEntity(_tmpId,_tmpHabitId,_tmpCompletedDate,_tmpCompletedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCompletion(habitId: String, date: String) {
    val _sql: String = "DELETE FROM habit_completions WHERE habitId = ? AND completedDate = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, habitId)
        _argIndex = 2
        _stmt.bindText(_argIndex, date)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteOldCompletions(cutoffDate: String) {
    val _sql: String = "DELETE FROM habit_completions WHERE completedDate < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, cutoffDate)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
