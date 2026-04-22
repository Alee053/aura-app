package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.tasks.await

class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseConfig.auth.currentUser?.uid
            if (userId == null) {
                return Result.failure()
            }

            val snapshot = FirebaseConfig.firestore
                .collection("users").document(userId).collection("todos")
                .whereEqualTo("isCompleted", false)
                .get()
                .await()

            val incompleteCount = snapshot.size()
            NotificationHelper.showDailySummaryNotification(applicationContext, incompleteCount)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "daily_summary_work"
    }
}