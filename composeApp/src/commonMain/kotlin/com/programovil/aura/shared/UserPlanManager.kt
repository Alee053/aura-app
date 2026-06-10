package com.programovil.aura.shared

import com.programovil.aura.experiments.domain.model.UserPlan
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Typed wrapper that exposes the current [UserPlan] (Free or Premium) derived
 * from the [FeatureFlag.IS_PREMIUM] flag in [FeatureFlagManager].
 *
 * [UserPlan] is a domain concept (Free/Premium today, more tiers later) sourced
 * from a single boolean remote-config flag. This class owns the mapping.
 */
@Mockable
class UserPlanManager(
    featureFlagManager: FeatureFlagManager
) {
    val userPlan: Flow<UserPlan> = featureFlagManager.flags
        .map { flags -> UserPlan.fromRemoteConfigBoolean(flags[FeatureFlag.IS_PREMIUM]) }
}
