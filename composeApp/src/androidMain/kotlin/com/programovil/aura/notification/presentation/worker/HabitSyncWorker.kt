package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.habit.data.mapper.HabitMapper.toDomain
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HabitSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val database: HabitDatabase by inject()

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseConfig.auth.currentUser?.uid
            if (userId == null) {
                return Result.failure()
            }

            val habitDao = database.habitDao()
            val pendingHabits = habitDao.getUnsyncedHabits()

            if (pendingHabits.isEmpty()) {
                return Result.success()
            }

            var syncedCount = 0
            for (habitEntity in pendingHabits) {
                val habit = habitEntity.toDomain()
                val data = hashMapOf(
                    "id" to habit.id,
                    "name" to habit.name,
                    "recurrenceType" to habit.recurrenceType.name,
                    "daysOfWeek" to habit.daysOfWeek.joinToString(","),
                    "color" to habit.color,
                    "createdAt" to habit.createdAt
                )

                FirebaseConfig.firestore
                    .collection("users").document(userId).collection("habits")
                    .document(habit.id)
                    .set(data)
                    .await()

                habitDao.markAsSynced(habit.id)
                syncedCount++
            }

            if (syncedCount > 0) {
                NotificationHelper.showSyncCompleteNotification(applicationContext, syncedCount)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "habit_sync_work"
    }
}
