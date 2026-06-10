package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Single source of truth for "can this user see the Habits feature?".
 *
 * Both signals must be true:
 *   - [FeatureFlag.HABITS_ENABLED] is the global kill switch.
 *   - The user has [UserPlan.Premium] (entitlement).
 *
 * Consumers should subscribe via [invoke] rather than re-deriving the rule
 * from the two flows.
 */
@Mockable
class GetHabitsAccessibilityUseCase(
    private val featureFlagManager: FeatureFlagManager,
    private val getUserPlanUseCase: GetUserPlanUseCase
) {
    operator fun invoke(): Flow<Boolean> = combine(
        featureFlagManager.flags,
        getUserPlanUseCase()
    ) { flags, plan ->
        (flags[FeatureFlag.HABITS_ENABLED] ?: true) && plan is UserPlan.Premium
    }
}
