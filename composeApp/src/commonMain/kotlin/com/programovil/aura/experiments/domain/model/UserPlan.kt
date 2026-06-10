package com.programovil.aura.experiments.domain.model

sealed class UserPlan {
    data object Free : UserPlan()
    data object Premium : UserPlan()

    companion object {
        fun fromRemoteConfigString(raw: String?): UserPlan = when (raw) {
            "Premium" -> Premium
            else -> Free
        }
    }
}
