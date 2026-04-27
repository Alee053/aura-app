package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitDao
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.habit.data.mapper.HabitMapper.toDomain
import com.programovil.aura.habit.data.mapper.HabitMapper.toEntity
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.sync.data.repository.IFirestoreSyncService
import com.programovil.aura.sync.data.repository.createFirestoreSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class HabitRepositoryImpl(
    private val database: HabitDatabase,
    private val syncService: IFirestoreSyncService
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

        val habitData = mapOf(
            "name" to habit.name,
            "recurrenceType" to habit.recurrenceType.name,
            "daysOfWeek" to habit.daysOfWeek.joinToString(","),
            "color" to habit.color,
            "createdAt" to habit.createdAt
        )

        // Try immediate sync first, if fails enqueue to sync_queue
        val success = syncService.syncHabit(habitData, habit.id, "CREATE")
        if (!success) {
            syncService.enqueueSync("HABIT", habit.id, "CREATE", habitData.toString())
        } else {
            // Direct success notification
            syncService.showSyncNotification(1, 0)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        habitDao.deleteHabit(habitId)

        val success = syncService.syncHabit(emptyMap(), habitId, "DELETE")
        if (!success) {
            syncService.enqueueSync("HABIT", habitId, "DELETE", "{}")
        }
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        val habitCompletionDao = database.habitCompletionDao()
        val existing = habitCompletionDao.getCompletionsForDateSync(date)
            .find { it.habitId == habitId }

        val isDeleting = existing != null
        val completionId: String

        if (existing != null) {
            habitCompletionDao.deleteCompletion(habitId, date)
            completionId = "${habitId}_$date"
        } else {
            val completion = HabitCompletion(
                id = randomUUID(),
                habitId = habitId,
                completedDate = date,
                completedAt = Clock.System.now().toEpochMilliseconds()
            )
            habitCompletionDao.insertCompletion(completion.toEntity())
            completionId = completion.id
        }

        val completionData = mapOf(
            "habitId" to habitId,
            "completedDate" to date,
            "completedAt" to Clock.System.now().toEpochMilliseconds()
        )

        val action = if (isDeleting) "DELETE" else "CREATE"
        val success = syncService.syncCompletion(completionData, completionId, action)
        if (!success) {
            syncService.enqueueSync("COMPLETION", completionId, action, completionData.toString())
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