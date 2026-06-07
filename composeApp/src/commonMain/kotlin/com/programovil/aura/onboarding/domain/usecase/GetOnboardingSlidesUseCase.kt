package com.programovil.aura.onboarding.domain.usecase

import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository

class GetOnboardingSlidesUseCase(
    private val repository: OnboardingRepository
) {
    suspend operator fun invoke(): Result<List<OnboardingSlide>> =
        repository.getSlides()
}
