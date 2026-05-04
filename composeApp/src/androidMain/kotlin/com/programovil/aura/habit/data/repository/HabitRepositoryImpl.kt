package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.mapper.HabitCompletionData
import com.programovil.aura.habit.data.mapper.HabitData
import com.programovil.aura.habit.data.mapper.toDomain
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual fun createHabitRepository(): HabitRepository = HabitRepositoryImpl()

private class HabitRepositoryImpl : HabitRepository {

    private val userId: String
        get() = FirebaseConfig.auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userHabitsCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("habits")

    private fun userCompletionsCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("completions")

    override fun getHabits(): Flow<Result<List<Habit>>> = callbackFlow {
        val listener = userHabitsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val habits = snapshot?.documents?.mapNotNull { doc ->
                    val daysOfWeek = (doc.get("daysOfWeek") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
                    val habitData = HabitData(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        recurrenceType = doc.getString("recurrenceType") ?: "DAILY",
                        daysOfWeek = daysOfWeek,
                        color = doc.getString("color") ?: "",
                        createdAt = doc.getLong("createdAt")
                    )
                    habitData.toDomain()
                } ?: emptyList()
                trySend(Result.success(habits))
            }
        awaitClose { listener.remove() }
    }

    override fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>> = callbackFlow {
        val listener = userCompletionsCollection()
            .whereEqualTo("habitId", habitId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val completions = snapshot?.documents?.mapNotNull { doc ->
                    val completionData = HabitCompletionData(
                        id = doc.id,
                        habitId = doc.getString("habitId") ?: "",
                        completedDate = doc.getString("completedDate") ?: "",
                        completedAt = doc.getLong("completedAt")
                    )
                    completionData.toDomain()
                } ?: emptyList()
                trySend(Result.success(completions))
            }
        awaitClose { listener.remove() }
    }

    override fun getAllCompletions(): Flow<Result<List<HabitCompletion>>> = callbackFlow {
        val listener = userCompletionsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val completions = snapshot?.documents?.mapNotNull { doc ->
                    val completionData = HabitCompletionData(
                        id = doc.id,
                        habitId = doc.getString("habitId") ?: "",
                        completedDate = doc.getString("completedDate") ?: "",
                        completedAt = doc.getLong("completedAt")
                    )
                    completionData.toDomain()
                } ?: emptyList()
                trySend(Result.success(completions))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addHabit(habit: Habit): Result<Unit> = runCatching {
        val data = mapOf(
            "name" to habit.name,
            "recurrenceType" to habit.recurrenceType.name,
            "daysOfWeek" to habit.daysOfWeek,
            "color" to habit.color,
            "createdAt" to habit.createdAt
        )
        userHabitsCollection().document(habit.id).set(data).await()
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        userHabitsCollection().document(habitId).delete().await()
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        val query = userCompletionsCollection()
            .whereEqualTo("habitId", habitId)
            .whereEqualTo("completedDate", date)
            .get()
            .await()

        if (!query.isEmpty) {
            query.documents.first().reference.delete().await()
        } else {
            val data = mapOf(
                "habitId" to habitId,
                "completedDate" to date,
                "completedAt" to System.currentTimeMillis()
            )
            userCompletionsCollection().add(data).await()
        }
    }
}
