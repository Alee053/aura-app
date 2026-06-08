package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.experiments.domain.usecase.GetNotificationVariantUseCase
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseConfig.auth.currentUser?.uid
            if (userId == null) {
                return Result.failure()
            }

            val incompleteCount = withTimeout(30_000) {
                FirebaseConfig.firestore
                    .collection("users").document(userId).collection("todos")
                    .whereEqualTo("isCompleted", false)
                    .get()
                    .await()
                    .size()
            }
            NotificationHelper.showDailySummaryNotification(applicationContext, incompleteCount)

            val useDueDate = runCatching {
                val variantUseCase: GetNotificationVariantUseCase = get()
                variantUseCase().useDueDateChannel
            }.getOrDefault(false)
            if (useDueDate) {
                val now = System.currentTimeMillis()
                val in24h = now + 24L * 60L * 60L * 1000L
                val dueTodos = FirebaseConfig.firestore
                    .collection("users").document(userId).collection("todos")
                    .whereGreaterThan("dueDate", now)
                    .whereLessThan("dueDate", in24h)
                    .whereEqualTo("isCompleted", false)
                    .get()
                    .await()
                val first = dueTodos.documents.firstOrNull()
                val title = first?.getString("title") ?: "Task"
                NotificationHelper.showDueDateNotification(applicationContext, title)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "daily_summary_work"
    }
}
