package com.programovil.aura.sync.data.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.sync.data.local.entity.EntityType
import com.programovil.aura.sync.data.local.entity.SyncAction
import com.programovil.aura.sync.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class SyncQueueWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        if (!isNetworkAvailable()) {
            return Result.retry()
        }

        val userId = FirebaseConfig.auth.currentUser?.uid
        if (userId == null) {
            return Result.failure()
        }

        val firestore = FirebaseConfig.firestore
        val pendingItems = getPendingItemsSync()

        if (pendingItems.isEmpty()) {
            return Result.success()
        }

        var syncedCount = 0
        var failedCount = 0

        for (item in pendingItems) {
            try {
                val success = processItem(firestore, userId, item)
                if (success) {
                    markCompleted(item.id)
                    syncedCount++
                } else {
                    incrementRetryCount(item.id)
                    failedCount++
                }
            } catch (e: Exception) {
                incrementRetryCount(item.id)
                failedCount++
            }
        }

        NotificationHelper.showSyncSummaryNotification(
            applicationContext,
            syncedCount,
            failedCount
        )

        return Result.success()
    }

    private fun getDatabase() = get<com.programovil.aura.habit.data.local.HabitDatabase>()

    private fun getPendingItemsSync(): List<SyncQueueEntity> {
        val dao = getDatabase().syncQueueDao()
        return kotlinx.coroutines.runBlocking {
            dao.getPendingItemsSync()
        }
    }

    private fun markCompleted(id: String) {
        val dao = getDatabase().syncQueueDao()
        kotlinx.coroutines.runBlocking {
            dao.markCompleted(id)
        }
    }

    private fun incrementRetryCount(id: String) {
        val dao = getDatabase().syncQueueDao()
        kotlinx.coroutines.runBlocking {
            dao.incrementRetryCount(id)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun processItem(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity): Boolean {
        if (item.retryCount >= MAX_RETRIES) {
            return false
        }

        val entityType = item.entityType
        val action = item.action

        return when (entityType) {
            EntityType.HABIT.name -> processHabit(firestore, userId, item, action)
            EntityType.TODO.name -> processTodo(firestore, userId, item, action)
            EntityType.COMPLETION.name -> processCompletion(firestore, userId, item, action)
            else -> false
        }
    }

    private suspend fun processHabit(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity, action: String): Boolean {
        val collection = firestore.collection("users").document(userId).collection("habits")

        return try {
            when (action) {
                SyncAction.CREATE.name, SyncAction.UPDATE.name -> {
                    val data = parseJson(item.data)
                    if (action == SyncAction.CREATE.name) {
                        collection.document(item.entityId).set(data).await()
                    } else {
                        collection.document(item.entityId).update(data).await()
                    }
                    true
                }
                SyncAction.DELETE.name -> {
                    collection.document(item.entityId).delete().await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun processTodo(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity, action: String): Boolean {
        val collection = firestore.collection("users").document(userId).collection("todos")

        return try {
            when (action) {
                SyncAction.CREATE.name -> {
                    val data = parseJson(item.data).toMutableMap()
                    data["createdAt"] = FieldValue.serverTimestamp()
                    collection.add(data).await()
                    true
                }
                SyncAction.UPDATE.name -> {
                    val data = parseJson(item.data)
                    collection.document(item.entityId).update(data).await()
                    true
                }
                SyncAction.DELETE.name -> {
                    collection.document(item.entityId).delete().await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun processCompletion(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity, action: String): Boolean {
        val collection = firestore.collection("users").document(userId).collection("completions")

        return try {
            when (action) {
                SyncAction.CREATE.name -> {
                    val data = parseJson(item.data)
                    collection.document(item.entityId).set(data).await()
                    true
                }
                SyncAction.DELETE.name -> {
                    collection.document(item.entityId).delete().await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseJson(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        try {
            val cleanJson = json.trim().removeSurrounding("{", "}")
            cleanJson.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"", "'")
                    val value = parts[1].trim().removeSurrounding("\"", "'")
                    result[key] = value
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        return result
    }

    companion object {
        const val WORK_NAME = "sync_queue_work"
        private const val MAX_RETRIES = 3
    }
}