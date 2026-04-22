package com.programovil.aura.habit.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.habit.sync.HabitSyncWorker
import com.programovil.aura.habit.sync.HabitSyncRunner
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private class AndroidHabitSyncScheduler(
    private val context: Context,
    private val habitDatabase: HabitDatabase
) : HabitSyncScheduler {

    override fun enqueueSync() {
        Log.d("HabitSyncWorker", "enqueueSync() called after local habit insert")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<HabitSyncWorker>()
            .setConstraints(constraints)
            .addTag(HabitSyncWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            HabitSyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override suspend fun syncNow(): Boolean {
        Log.d("HabitSyncWorker", "Attempting immediate habit sync")
        val result = HabitSyncRunner.syncPendingHabits(
            context = context,
            habitDatabase = habitDatabase
        )
        return result.allSynced
    }
}

actual fun createHabitSyncScheduler(): HabitSyncScheduler {
    val deps = object : KoinComponent {
        val appContext: Context = get()
        val database: HabitDatabase = get()
    }
    return AndroidHabitSyncScheduler(
        context = deps.appContext,
        habitDatabase = deps.database
    )
}
