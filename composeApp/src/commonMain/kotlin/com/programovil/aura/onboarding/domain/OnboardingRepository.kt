package com.programovil.aura.onboarding.domain

import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    fun getSlides(): Flow<Result<List<OnboardingSlide>>>
    suspend fun hasSeenOnboarding(): Boolean
    suspend fun setOnboarded(seen: Boolean)
}
