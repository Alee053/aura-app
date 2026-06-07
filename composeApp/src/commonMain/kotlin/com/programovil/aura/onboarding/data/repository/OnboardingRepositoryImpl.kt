package com.programovil.aura.onboarding.data.repository

import com.programovil.aura.onboarding.data.dto.OnboardingConfigDto
import com.programovil.aura.onboarding.data.getSystemLocale
import com.programovil.aura.onboarding.data.mapper.toDomain
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import aura_app.composeapp.generated.resources.Res

class OnboardingRepositoryImpl : OnboardingRepository {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getSlides(): Result<List<OnboardingSlide>> {
        return try {
            val jsonString = Res.readBytes("files/onboarding_config.json")
                .decodeToString()
            val dto = json.decodeFromString<OnboardingConfigDto>(jsonString)
            val locale = getSystemLocale()
            Result.success(dto.slides.map { it.toDomain(locale) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}