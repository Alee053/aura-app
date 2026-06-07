package com.programovil.aura.onboarding.data.repository

import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    @Test
    fun `getSlides returns a Result type`() = runTest {
        val repository: OnboardingRepository = OnboardingRepositoryImpl()
        val result = repository.getSlides()
        assertTrue(result is kotlin.Result<*>) // Verifies the contract returns a Result
    }

    @Test
    fun `getSlides parses valid JSON from resources`() = runTest {
        val repository = OnboardingRepositoryImpl()
        val result = repository.getSlides()
        assertTrue(result.isSuccess)
        val slides = result.getOrElse { emptyList() }
        assertTrue(slides.isNotEmpty())
        assertTrue(slides.size == 4)
    }

    @Test
    fun `getSlides returns slides with resolved text`() = runTest {
        val repository = OnboardingRepositoryImpl()
        val result = repository.getSlides()
        val slides = result.getOrElse { emptyList() }
        assertTrue(slides.all { it.title.isNotBlank() })
        assertTrue(slides.all { it.description.isNotBlank() })
    }

    @Test
    fun `getSlides returns slides with correct ids`() = runTest {
        val repository = OnboardingRepositoryImpl()
        val result = repository.getSlides()
        val slides = result.getOrElse { emptyList() }
        assertTrue(slides.map { it.id } == listOf(1, 2, 3, 4))
    }
}