package com.programovil.aura.shared

import com.programovil.aura.experiments.domain.model.UserPlan
import io.mockative.Mockable
import kotlinx.coroutines.flow.StateFlow

/**
 * Typed wrapper that exposes the current [UserPlan] (Free or Premium) sourced
 * from Remote Config. Internally owns a [RemoteConfigValueManager]; no polling
 * loop is started.
 */
@Mockable
class UserPlanManager(
    remoteConfigService: RemoteConfigService
) {
    private val manager = RemoteConfigValueManager(
        remoteConfigService = remoteConfigService,
        key = UserPlanFlag.USER_PLAN.key,
        defaultValue = UserPlan.fromRemoteConfigString(UserPlanFlag.USER_PLAN.defaultValue),
        parser = { raw -> UserPlan.fromRemoteConfigString(raw) }
    )

    val userPlan: StateFlow<UserPlan> = manager.value

    suspend fun initialize() = manager.initialize()

    fun refresh() = manager.refresh()

    fun stop() = manager.stop()
}
