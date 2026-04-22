package com.programovil.aura.habit.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.habit.data.local.entity.HabitEntity
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.tasks.await

data class HabitSyncResult(
    val allSynced: Boolean,
    val syncedCount: Int,
    val pendingCount: Int
)

object HabitSyncRunner {
    private const val TAG = "HabitSyncWorker"

    suspend fun syncPendingHabits(
        context: Context,
        habitDatabase: HabitDatabase
    ): HabitSyncResult {
        Log.d(TAG, "Habit sync started")

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d(TAG, "No authenticated user found. Aborting sync.")
            return HabitSyncResult(allSynced = false, syncedCount = 0, pendingCount = 0)
        }

        Log.d(TAG, "Using UID for sync: $userId")

        val habitDao = habitDatabase.habitDao()
        val pendingHabits = habitDao.getPendingSyncHabits()
        Log.d(TAG, "Pending habits found: ${pendingHabits.size}")

        if (pendingHabits.isEmpty()) {
            Log.d(TAG, "No pending habits to sync")
            return HabitSyncResult(allSynced = true, syncedCount = 0, pendingCount = 0)
        }

        var syncedCount = 0
        var hasFailure = false

        pendingHabits.forEach { habit ->
            Log.d(TAG, "Uploading habit '${habit.id}' to Firestore")

            runCatching {
                FirebaseConfig.firestore
                    .collection("users")
                    .document(userId)
                    .collection("habits")
                    .document(habit.id)
                    .set(habit.toSyncPayload())
                    .await()

                Log.d(TAG, "Firestore upload succeeded for habit '${habit.id}'")

                habitDao.updateSyncPendingState(
                    habitId = habit.id,
                    isSyncPending = false
                )
                syncedCount++
                Log.d(TAG, "Marked habit '${habit.id}' as synchronized in Room")
            }.onFailure { throwable ->
                hasFailure = true
                Log.d(
                    TAG,
                    "Firestore upload failed for habit '${habit.id}': ${throwable.message}"
                )
            }
        }

        if (syncedCount > 0) {
            NotificationHelper.showHabitSyncNotification(context, syncedCount)
        }

        return HabitSyncResult(
            allSynced = !hasFailure,
            syncedCount = syncedCount,
            pendingCount = pendingHabits.size
        )
    }

    private fun HabitEntity.toSyncPayload(): HabitSyncPayload = HabitSyncPayload(
        id = id,
        name = name,
        recurrenceType = recurrenceType,
        daysOfWeek = if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map(String::toInt),
        color = color,
        createdAt = createdAt
    )

    data class HabitSyncPayload(
        val id: String = "",
        val name: String = "",
        val recurrenceType: String = "",
        val daysOfWeek: List<Int> = emptyList(),
        val color: String = "",
        val createdAt: Long = 0L
    )
}
