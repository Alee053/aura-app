package com.programovil.aura.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.notification.NotificationHelper
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val db: HabitDatabase by inject()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    override suspend fun doWork(): Result {
        val dao = db.syncQueueDao() // Asegúrate de agregar el DAO a HabitDatabase
        val pendingItems = dao.getAllPending()

        if (pendingItems.isEmpty()) return Result.success()

        var successCount = 0
        pendingItems.forEach { item ->
            try {
                // Subir a Realtime Database bajo el nodo "sync_queue/userId/..."
                realtimeDb.child("sync_queue").child(item.id).setValue(item.payloadJson).await()
                dao.removeFromQueue(item.id)
                successCount++
            } catch (e: Exception) {
                return Result.retry()
            }
        }

        if (successCount > 0) {
            NotificationHelper.showHabitSyncNotification(applicationContext, successCount)
        }

        return Result.success()
    }
}
