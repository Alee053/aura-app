package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
class GetUserPlanUseCase(private val repository: UserPlanRepository) {
    operator fun invoke(): Flow<UserPlan> = repository.observeUserPlan()
    suspend fun get(): UserPlan = repository.getUserPlan()
    suspend fun set(plan: UserPlan) = repository.setUserPlan(plan)
    suspend fun refresh(): Result<Unit> = repository.refreshFromRemote()
}
