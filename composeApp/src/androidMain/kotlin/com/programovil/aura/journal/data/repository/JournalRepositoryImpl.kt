package com.programovil.aura.journal.domain.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.programovil.aura.journal.data.entity.JournalEntity
import com.programovil.aura.journal.data.mapper.toDomain
import com.programovil.aura.journal.data.mapper.toEntity
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual fun createJournalRepository(): JournalRepository = AndroidJournalRepositoryImpl()

private class AndroidJournalRepositoryImpl : JournalRepository {

    private val userId: String
        get() = FirebaseConfig.auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userJournalCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("journals")

    override fun getEntries(): Flow<List<JournalEntry>> = callbackFlow {
        val listener = userJournalCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { document -> document.toJournalEntity() }.orEmpty()
                    .map(JournalEntity::toDomain)
                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getEntry(id: String): JournalEntry? =
        userJournalCollection()
            .document(id)
            .get()
            .await()
            .toJournalEntity()
            ?.toDomain()

    override suspend fun addEntry(title: String, content: String): Result<Unit> = runCatching {
        val document = userJournalCollection().document()
        val now = System.currentTimeMillis()
        val entity = JournalEntity(
            id = document.id,
            title = title,
            content = content,
            createdAt = now,
            updatedAt = now
        )
        document.set(entity.toFirestoreData()).await()
    }

    override suspend fun updateEntry(entry: JournalEntry): Result<Unit> = runCatching {
        val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis()).toEntity()
        userJournalCollection().document(updatedEntry.id).set(updatedEntry.toFirestoreData()).await()
    }

    override suspend fun deleteEntry(entry: JournalEntry): Result<Unit> = runCatching {
        userJournalCollection().document(entry.id).delete().await()
    }
}

private fun DocumentSnapshot.toJournalEntity(): JournalEntity? {
    val title = getString("title") ?: return null
    val content = getString("content") ?: return null
    val createdAt = getLong("createdAt") ?: return null
    val updatedAt = getLong("updatedAt") ?: createdAt

    return JournalEntity(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun JournalEntity.toFirestoreData(): Map<String, Any> = mapOf(
    "title" to title,
    "content" to content,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)
