package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitDao
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.habit.data.mapper.HabitMapper.toDomain
import com.programovil.aura.habit.data.mapper.HabitMapper.toEntity
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class HabitRepositoryImpl(
    private val database: HabitDatabase
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
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        habitDao.deleteHabit(habitId)
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        val habitCompletionDao = database.habitCompletionDao()
        val existing = habitCompletionDao.getCompletionsForDate(date).first()
            .find { it.habitId == habitId }
        if (existing != null) {
            habitCompletionDao.deleteCompletion(habitId, date)
        } else {
            val completion = HabitCompletion(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                completedDate = date,
                completedAt = System.currentTimeMillis()
            )
            habitCompletionDao.insertCompletion(completion.toEntity())
        }
    }

    override suspend fun cleanupOldCompletions(olderThanDays: Int): Result<Unit> = runCatching {
        val cutoffDate = java.time.LocalDate.now().minusDays(olderThanDays.toLong())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        database.habitCompletionDao().deleteOldCompletions(cutoffDate)
    }
}
