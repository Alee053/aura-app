package com.programovil.aura.habit.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.programovil.aura.habit.`data`.local.entity.HabitEntity
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
public class HabitDao_Impl(
  __db: RoomDatabase,
) : HabitDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfHabitEntity: EntityInsertAdapter<HabitEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfHabitEntity = object : EntityInsertAdapter<HabitEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `habits` (`id`,`name`,`recurrenceType`,`daysOfWeek`,`color`,`createdAt`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: HabitEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.recurrenceType)
        statement.bindText(4, entity.daysOfWeek)
        statement.bindText(5, entity.color)
        statement.bindLong(6, entity.createdAt)
      }
    }
  }

  public override suspend fun insertHabit(habit: HabitEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfHabitEntity.insert(_connection, habit)
  }

  public override fun getAllHabits(): Flow<List<HabitEntity>> {
    val _sql: String = "SELECT * FROM habits ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("habits")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfRecurrenceType: Int = getColumnIndexOrThrow(_stmt, "recurrenceType")
        val _columnIndexOfDaysOfWeek: Int = getColumnIndexOrThrow(_stmt, "daysOfWeek")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<HabitEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpRecurrenceType: String
          _tmpRecurrenceType = _stmt.getText(_columnIndexOfRecurrenceType)
          val _tmpDaysOfWeek: String
          _tmpDaysOfWeek = _stmt.getText(_columnIndexOfDaysOfWeek)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = HabitEntity(_tmpId,_tmpName,_tmpRecurrenceType,_tmpDaysOfWeek,_tmpColor,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteHabit(habitId: String) {
    val _sql: String = "DELETE FROM habits WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, habitId)
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
