package com.programovil.aura.habit.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.habit.data.local.HabitDatabase
import org.koin.core.context.GlobalContext

class HabitSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val habitDatabase: HabitDatabase
        get() = GlobalContext.get().get()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Habit sync worker started")
        val result = HabitSyncRunner.syncPendingHabits(
            context = applicationContext,
            habitDatabase = habitDatabase
        )
        return if (result.allSynced) {
            Log.d(TAG, "Habit sync completed successfully")
            Result.success()
        } else {
            Log.d(TAG, "Habit sync completed with failures")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HabitSyncWorker"
        const val UNIQUE_WORK_NAME = "habit_sync_work"
        const val WORK_TAG = "habit_sync"
    }
}
