package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitDao
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.habit.data.mapper.HabitMapper.toDomain
import com.programovil.aura.habit.data.mapper.HabitMapper.toEntity
import com.programovil.aura.habit.data.sync.HabitSyncScheduler
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class HabitRepositoryImpl(
    private val database: HabitDatabase,
    private val habitSyncScheduler: HabitSyncScheduler
) : HabitRepository {

    private val habitDao: HabitDao get() = database.habitDao()

    override fun getHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
        return database.habitCompletionDao().getCompletionsForHabit(habitId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllCompletions(): Flow<List<HabitCompletion>> {
        return database.habitCompletionDao().getAllCompletions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addHabit(habit: Habit): Result<Unit> = runCatching {
        habitDao.insertHabit(habit.toEntity())
        val syncedImmediately = habitSyncScheduler.syncNow()
        if (!syncedImmediately) {
            habitSyncScheduler.enqueueSync()
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        habitDao.deleteHabit(habitId)
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        val habitCompletionDao = database.habitCompletionDao()
        val existing = habitCompletionDao.getCompletionsForDateSync(date)
            .find { it.habitId == habitId }
        
        if (existing != null) {
            habitCompletionDao.deleteCompletion(habitId, date)
        } else {
            val completion = HabitCompletion(
                id = randomUUID(),
                habitId = habitId,
                completedDate = date,
                completedAt = Clock.System.now().toEpochMilliseconds()
            )
            habitCompletionDao.insertCompletion(completion.toEntity())
        }
    }

    override suspend fun cleanupOldCompletions(olderThanDays: Int): Result<Unit> = runCatching {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val cutoffDate = today.minus(olderThanDays, DateTimeUnit.DAY).toString()
        database.habitCompletionDao().deleteOldCompletions(cutoffDate)
    }

    private fun randomUUID(): String {
        return "hc-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
    }
}
