package com.programovil.aura.onboarding.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingSlide(
    val id: Int,
    val title: Map<String, String>,
    val description: Map<String, String>,
    val image_url: Map<String, String>
)

@Serializable
data class OnboardingConfig(
    val onboarding_config: List<OnboardingSlide>
)
