package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.domain.model.UserPlan
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
interface UserPlanRepository {
    fun observeUserPlan(): Flow<UserPlan>
    suspend fun getUserPlan(): UserPlan
    suspend fun setUserPlan(plan: UserPlan)
    suspend fun refreshFromRemote(): Result<Unit>
}

expect fun createUserPlanRepository(): UserPlanRepository
