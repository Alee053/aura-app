package com.programovil.aura.sync.data.repository

import android.content.Context
import com.programovil.aura.habit.data.local.HabitDatabase
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.sync.data.local.entity.EntityType
import com.programovil.aura.sync.data.local.entity.SyncAction
import com.programovil.aura.sync.data.local.entity.SyncQueueEntity
import com.programovil.aura.sync.data.worker.SyncScheduler
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FirestoreSyncService(
    private val database: HabitDatabase? = null
) : IFirestoreSyncService, KoinComponent {
    private val context: Context by inject()
    private val firestore = FirebaseConfig.firestore
    private val userId: String? get() = FirebaseConfig.auth.currentUser?.uid

    override suspend fun syncHabit(habitData: Map<String, Any>, habitId: String, action: String): Boolean {
        return try {
            val uid = userId ?: return false
            val collection = firestore.collection("users").document(uid).collection("habits")

            when (action) {
                "CREATE" -> collection.document(habitId).set(habitData).await()
                "UPDATE" -> collection.document(habitId).update(habitData).await()
                "DELETE" -> collection.document(habitId).delete().await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncTodo(todoData: Map<String, Any>, todoId: String?, action: String): Boolean {
        return try {
            val uid = userId ?: return false
            val collection = firestore.collection("users").document(uid).collection("todos")

            when (action) {
                "CREATE" -> {
                    if (todoId != null) {
                        collection.document(todoId).set(todoData).await()
                    } else {
                        collection.add(todoData).await()
                    }
                }
                "UPDATE" -> {
                    if (todoId != null) {
                        collection.document(todoId).update(todoData).await()
                    }
                }
                "DELETE" -> {
                    if (todoId != null) {
                        collection.document(todoId).delete().await()
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun syncCompletion(completionData: Map<String, Any>, completionId: String, action: String): Boolean {
        return try {
            val uid = userId ?: return false
            val collection = firestore.collection("users").document(uid).collection("completions")

            when (action) {
                "CREATE" -> collection.document(completionId).set(completionData).await()
                "DELETE" -> collection.document(completionId).delete().await()
                else -> false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun enqueueSync(entityType: String, entityId: String, action: String, data: String): Boolean {
        // Try Firestore first
        try {
            val uid = userId ?: return false
            val syncCollection = firestore.collection("users").document(uid).collection("sync_queue")
            val syncItem = mapOf(
                "entityType" to entityType,
                "entityId" to entityId,
                "action" to action,
                "data" to data,
                "pending" to true,
                "retryCount" to 0,
                "createdAt" to System.currentTimeMillis()
            )
            syncCollection.add(syncItem).await()
            return true
        } catch (e: Exception) {
            // Firestore failed, save to local Room database
        }

        // Fallback to local Room database
        val result = saveToLocalQueue(entityType, entityId, action, data)
        if (result) {
            SyncScheduler.triggerImmediateSync(context)
        }
        return result
    }

    private suspend fun saveToLocalQueue(entityType: String, entityId: String, action: String, data: String): Boolean {
        return try {
            database?.syncQueueDao()?.let { dao ->
                val item = SyncQueueEntity(
                    id = "sync-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}",
                    entityType = entityType,
                    entityId = entityId,
                    action = action,
                    data = data,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    pending = true,
                    retryCount = 0
                )
                dao.insert(item)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}

actual fun createFirestoreSyncService(database: HabitDatabase): IFirestoreSyncService =
    FirestoreSyncService(database)
