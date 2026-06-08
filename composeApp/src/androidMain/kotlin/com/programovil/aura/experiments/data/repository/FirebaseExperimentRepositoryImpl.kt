package com.programovil.aura.experiments.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database
import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import kotlinx.coroutines.tasks.await

class FirebaseExperimentRepositoryImpl : ExperimentRepository {

    private val database by lazy { Firebase.database }
    private val auth by lazy { Firebase.auth }

    override suspend fun logEvent(event: ExperimentEvent): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
            ?: error("Cannot log experiment event: no authenticated user")
        val ref = database.reference
            .child("users")
            .child(uid)
            .child("experiments")
            .child("events")
            .push()
        val payload = mapOf(
            "type" to eventTypeName(event),
            "variant" to event.userPlan.variantName(),
            "timestamp" to ServerValue.TIMESTAMP,
            "metadata" to eventMetadata(event)
        )
        ref.setValue(payload).await()
    }

    private fun eventTypeName(event: ExperimentEvent): String = when (event) {
        is ExperimentEvent.HomeOpened -> "home_opened"
        is ExperimentEvent.TabClicked -> "tab_clicked"
        is ExperimentEvent.NotificationDelivered -> "notification_delivered"
        is ExperimentEvent.SessionActive -> "session_active"
    }

    private fun eventMetadata(event: ExperimentEvent): Map<String, String> = when (event) {
        is ExperimentEvent.TabClicked -> mapOf("tab" to event.tabName)
        is ExperimentEvent.NotificationDelivered -> mapOf("channel" to event.channel)
        else -> emptyMap()
    }

    private fun UserPlan.variantName(): String = when (this) {
        UserPlan.Premium -> "Premium"
        UserPlan.Free -> "Free"
    }
}
