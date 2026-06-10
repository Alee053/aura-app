package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createUserPlanRepository(): UserPlanRepository = object : UserPlanRepository {
    override fun observeUserPlan(): Flow<UserPlan> = flowOf(UserPlan.Free)
    override suspend fun getUserPlan(): UserPlan = UserPlan.Free
    override suspend fun setUserPlan(plan: UserPlan) {}
    override suspend fun refreshFromRemote(): Result<Unit> = Result.success(Unit)
}
