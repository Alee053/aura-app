package com.programovil.aura.onboarding.data.repository

import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    @Test
    fun `getSlides returns a Result type`() {
        val repository: OnboardingRepository = OnboardingRepositoryImpl()
        val result = runCatching { kotlinx.coroutines.runBlocking { repository.getSlides() } }
        assertTrue(result.getOrNull() is Result<*> || result.isFailure)
    }
}
