package com.programovil.aura.onboarding.data.mapper

import com.programovil.aura.onboarding.data.dto.OnboardingSlideDto
import com.programovil.aura.onboarding.domain.model.OnboardingSlide

internal fun OnboardingSlideDto.toDomain(locale: String): OnboardingSlide {
    return OnboardingSlide(
        id = id,
        title = title[locale] ?: title["en"] ?: "",
        description = description[locale] ?: description["en"] ?: "",
        imageUrl = imageUrl[locale]?.takeIf { it.isNotBlank() }
    )
}