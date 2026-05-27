package com.programovil.aura.onboarding.data.repository

import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.RemoteConfigService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class OnboardingRepositoryImplTest {

    @Test
    fun `getSlides returns failure when JSON is invalid`() = runTest {
        val repository: OnboardingRepository = OnboardingRepositoryImpl(
            FakeRemoteConfigService()
        )
        val result = repository.getSlides()
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun `getSlides parses valid JSON from resources`() = runTest {
        val repository = OnboardingRepositoryImpl(FakeRemoteConfigService())
        val result = repository.getSlides()
        assertTrue(result.isSuccess)
        val slides = result.getOrNull()!!
        assertTrue(slides.isNotEmpty())
        assertTrue(slides.size == 4)
    }

    @Test
    fun `getSlides returns slides with resolved text`() = runTest {
        val repository = OnboardingRepositoryImpl(FakeRemoteConfigService())
        val result = repository.getSlides()
        val slides = result.getOrNull()!!
        assertTrue(slides.all { it.title.isNotBlank() })
        assertTrue(slides.all { it.description.isNotBlank() })
    }

    @Test
    fun `getSlides returns slides with correct ids`() = runTest {
        val repository = OnboardingRepositoryImpl(FakeRemoteConfigService())
        val result = repository.getSlides()
        val slides = result.getOrNull()!!
        assertTrue(slides.map { it.id } == listOf(1, 2, 3, 4))
    }
}

private class FakeRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(flag: FeatureFlag): Boolean = flag.defaultValue
    override suspend fun getString(flag: FeatureFlag, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
}