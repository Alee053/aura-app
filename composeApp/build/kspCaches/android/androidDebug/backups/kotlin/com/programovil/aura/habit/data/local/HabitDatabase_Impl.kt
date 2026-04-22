package com.programovil.aura.habit.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HabitDatabase_Impl : HabitDatabase() {
  private val _habitDao: Lazy<HabitDao> = lazy {
    HabitDao_Impl(this)
  }

  private val _habitCompletionDao: Lazy<HabitCompletionDao> = lazy {
    HabitCompletionDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "73ab3dab4187fbc52c36387d9fea6a95", "3630de826acda39ca0410917391b1c0a") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `recurrenceType` TEXT NOT NULL, `daysOfWeek` TEXT NOT NULL, `color` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `habit_completions` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, `completedDate` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habitId` ON `habit_completions` (`habitId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_completedDate` ON `habit_completions` (`completedDate`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '73ab3dab4187fbc52c36387d9fea6a95')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `habits`")
        connection.execSQL("DROP TABLE IF EXISTS `habit_completions`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsHabits: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHabits.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("recurrenceType", TableInfo.Column("recurrenceType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("daysOfWeek", TableInfo.Column("daysOfWeek", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("color", TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHabits: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHabits: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoHabits: TableInfo = TableInfo("habits", _columnsHabits, _foreignKeysHabits, _indicesHabits)
        val _existingHabits: TableInfo = read(connection, "habits")
        if (!_infoHabits.equals(_existingHabits)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |habits(com.programovil.aura.habit.data.local.entity.HabitEntity).
              | Expected:
              |""".trimMargin() + _infoHabits + """
              |
              | Found:
              |""".trimMargin() + _existingHabits)
        }
        val _columnsHabitCompletions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHabitCompletions.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabitCompletions.put("habitId", TableInfo.Column("habitId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabitCompletions.put("completedDate", TableInfo.Column("completedDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabitCompletions.put("completedAt", TableInfo.Column("completedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHabitCompletions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHabitCompletions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesHabitCompletions.add(TableInfo.Index("index_habit_completions_habitId", false, listOf("habitId"), listOf("ASC")))
        _indicesHabitCompletions.add(TableInfo.Index("index_habit_completions_completedDate", false, listOf("completedDate"), listOf("ASC")))
        val _infoHabitCompletions: TableInfo = TableInfo("habit_completions", _columnsHabitCompletions, _foreignKeysHabitCompletions, _indicesHabitCompletions)
        val _existingHabitCompletions: TableInfo = read(connection, "habit_completions")
        if (!_infoHabitCompletions.equals(_existingHabitCompletions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |habit_completions(com.programovil.aura.habit.data.local.entity.HabitCompletionEntity).
              | Expected:
              |""".trimMargin() + _infoHabitCompletions + """
              |
              | Found:
              |""".trimMargin() + _existingHabitCompletions)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "habits", "habit_completions")
  }

  public override fun clearAllTables() {
    super.performClear(false, "habits", "habit_completions")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(HabitDao::class, HabitDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(HabitCompletionDao::class, HabitCompletionDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun habitDao(): HabitDao = _habitDao.value

  public override fun habitCompletionDao(): HabitCompletionDao = _habitCompletionDao.value
}
