package com.programovil.aura.habit

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.programovil.aura.habit.data.local.HabitDao
import com.programovil.aura.notification.NotificationHelper
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val habitDao: HabitDao by inject()
    private val database = Firebase.database.reference

    override suspend fun doWork(): Result {
        return try {
            val unsyncedHabits = habitDao.getUnsyncedHabits()
            
            if (unsyncedHabits.isEmpty()) return Result.success()

            for (habit in unsyncedHabits) {
                // Sincronización con Firebase Realtime Database
                database.child("habits").child(habit.id).setValue(habit).await()
                
                // Marcar como sincronizado en Room
                habitDao.markSynced(habit.id)
            }

            // Notificación local de éxito
            NotificationHelper.showNotification(
                applicationContext,
                "Sincronización Exitosa",
                "Se han subido ${unsyncedHabits.size} hábitos a la nube."
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
