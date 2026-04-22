package com.programovil.aura.sync.data.worker

import android.content.Context
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
        var syncedCount = 0
        var failedCount = 0

        return try {
            // Wait up to 3 seconds for Firebase Auth
            var userId = FirebaseConfig.auth.currentUser?.uid
            var attempts = 0
            while (userId == null && attempts < 3) {
                kotlinx.coroutines.delay(1000)
                userId = FirebaseConfig.auth.currentUser?.uid
                attempts++
            }

            val currentUserId = userId ?: return Result.failure()
            val firestore = FirebaseFirestore.getInstance()
            val database = get<com.programovil.aura.habit.data.local.HabitDatabase>()

            val localItems = getLocalPendingItems(database)
            val firestoreItems = getFirestorePendingItems(currentUserId)
            val allItems = (localItems + firestoreItems).associateBy { it.id }.values.toList()

            // Process items if any
            for (item in allItems) {
                try {
                    val success = processItem(firestore, currentUserId, item)
                    if (success) {
                        removeFromLocal(database, item.id)
                        removeFromFirestore(currentUserId, item.id)
                        syncedCount++
                    } else {
                        incrementRetryCount(database, item.id)
                        incrementFirestoreRetry(currentUserId, item.id)
                        failedCount++
                    }
                } catch (e: Exception) {
                    incrementRetryCount(database, item.id)
                    failedCount++
                }
            }

            // ALWAYS SHOW NOTIFICATION AT THE END
            NotificationHelper.showSyncSummaryNotification(
                applicationContext,
                syncedCount,
                failedCount
            )

            if (failedCount > 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            // Even on error, show what we have
            NotificationHelper.showSyncSummaryNotification(applicationContext, syncedCount, failedCount)
            Result.retry()
        }
    }

    private suspend fun getLocalPendingItems(database: com.programovil.aura.habit.data.local.HabitDatabase): List<SyncQueueEntity> {
        return database.syncQueueDao().getPendingItemsSync()
    }

    private suspend fun getFirestorePendingItems(userId: String): List<SyncQueueEntity> {
        return try {
            FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("sync_queue")
                .whereEqualTo("pending", true)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    SyncQueueEntity(
                        id = doc.id,
                        entityType = doc.getString("entityType") ?: return@mapNotNull null,
                        entityId = doc.getString("entityId") ?: return@mapNotNull null,
                        action = doc.getString("action") ?: return@mapNotNull null,
                        data = doc.getString("data") ?: "{}",
                        createdAt = doc.getLong("createdAt") ?: 0,
                        pending = true,
                        retryCount = doc.getLong("retryCount")?.toInt() ?: 0
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun removeFromLocal(database: com.programovil.aura.habit.data.local.HabitDatabase, id: String) {
        database.syncQueueDao().delete(id)
    }

    private suspend fun removeFromFirestore(userId: String, id: String) {
        try {
            FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("sync_queue").document(id)
                .delete()
                .await()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private suspend fun incrementRetryCount(database: com.programovil.aura.habit.data.local.HabitDatabase, id: String) {
        database.syncQueueDao().incrementRetryCount(id)
    }

    private suspend fun incrementFirestoreRetry(userId: String, id: String) {
        try {
            FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("sync_queue").document(id)
                .update("retryCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private suspend fun processItem(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity): Boolean {
        return when (item.entityType) {
            EntityType.HABIT.name -> processHabit(firestore, userId, item)
            EntityType.TODO.name -> processTodo(firestore, userId, item)
            EntityType.COMPLETION.name -> processCompletion(firestore, userId, item)
            else -> false
        }
    }

    private suspend fun processHabit(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity): Boolean {
        val collection = firestore.collection("users").document(userId).collection("habits")

        return try {
            when (item.action) {
                SyncAction.CREATE.name, SyncAction.UPDATE.name -> {
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

    private suspend fun processTodo(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity): Boolean {
        val collection = firestore.collection("users").document(userId).collection("todos")

        return try {
            when (item.action) {
                SyncAction.CREATE.name -> {
                    val data = parseJson(item.data).toMutableMap()
                    data["createdAt"] = FieldValue.serverTimestamp()
                    collection.document(item.entityId).set(data).await()
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

    private suspend fun processCompletion(firestore: FirebaseFirestore, userId: String, item: SyncQueueEntity): Boolean {
        val collection = firestore.collection("users").document(userId).collection("completions")

        return try {
            when (item.action) {
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
            if (cleanJson.isEmpty()) return result
            
            cleanJson.split(",").forEach { pair ->
                val parts = pair.split("=") // Kotlin Map.toString() uses '=' not ':'
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"", "'")
                    val value = parts[1].trim().removeSurrounding("\"", "'")
                    result[key] = value
                } else {
                    // Try with ':' just in case
                    val partsColon = pair.split(":")
                    if (partsColon.size == 2) {
                        val key = partsColon[0].trim().removeSurrounding("\"", "'")
                        val value = partsColon[1].trim().removeSurrounding("\"", "'")
                        result[key] = value
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        return result
    }

    companion object {
        const val WORK_NAME = "sync_queue_work"
    }
}
