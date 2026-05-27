package com.programovil.aura.onboarding.domain.repository

import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import io.mockative.Mockable

@Mockable
interface OnboardingRepository {
    suspend fun getSlides(): Result<List<OnboardingSlide>>
}
