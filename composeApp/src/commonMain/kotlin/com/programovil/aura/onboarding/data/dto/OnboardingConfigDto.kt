package com.programovil.aura.onboarding.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingConfigDto(
    @SerialName("onboarding_config") val slides: List<OnboardingSlideDto>
)

@Serializable
internal data class OnboardingSlideDto(
    val id: Int,
    val title: Map<String, String>,
    val description: Map<String, String>,
    @SerialName("image_url") val imageUrl: Map<String, String>
)