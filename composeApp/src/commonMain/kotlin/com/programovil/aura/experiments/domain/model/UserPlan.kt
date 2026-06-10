package com.programovil.aura.experiments.domain.model

sealed class UserPlan {
    data object Free : UserPlan()
    data object Premium : UserPlan()

    companion object {
        fun fromRemoteConfigBoolean(isPremium: Boolean?): UserPlan =
            if (isPremium == true) Premium else Free
    }
}
