package com.programovil.aura.experiments.domain.model

sealed class ExperimentEvent {
    abstract val userPlan: UserPlan

    data class HomeOpened(override val userPlan: UserPlan) : ExperimentEvent()
    data class TabClicked(override val userPlan: UserPlan, val tabName: String) : ExperimentEvent()
    data class NotificationDelivered(override val userPlan: UserPlan, val channel: String) : ExperimentEvent()
    data class SessionActive(override val userPlan: UserPlan) : ExperimentEvent()
}
