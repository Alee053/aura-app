package com.programovil.aura.onboarding.presentation

import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.domain.repository.OnboardingRepository
import com.programovil.aura.onboarding.domain.usecase.GetOnboardingSlidesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleSlides = listOf(
        OnboardingSlide(1, "Title 1", "Desc 1", null),
        OnboardingSlide(2, "Title 2", "Desc 2", null),
        OnboardingSlide(3, "Title 3", "Desc 3", null),
        OnboardingSlide(4, "Title 4", "Desc 4", null)
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        slides: Result<List<OnboardingSlide>> = Result.success(sampleSlides)
    ): OnboardingViewModel {
        val repository = FakeOnboardingRepository(slides)
        val useCase = GetOnboardingSlidesUseCase(repository)
        val prefs = FakeOnboardingPreferences()
        return OnboardingViewModel(useCase, prefs)
    }

    @Test
    fun `initial state is Loaded with first slide`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Loaded>(state)
        assertEquals(0, state.currentIndex)
        assertEquals(4, state.slides.size)
        assertEquals(false, state.isLastSlide)
    }

    @Test
    fun `next advances to next slide`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(1, state.currentIndex)
        assertEquals(false, state.isLastSlide)
    }

    @Test
    fun `next on last slide does nothing`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        vm.next()
        vm.next()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(3, state.currentIndex)
        assertEquals(true, state.isLastSlide)
        vm.next()
        val stateAfter = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(3, stateAfter.currentIndex)
    }

    @Test
    fun `previous goes back one slide`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        vm.next()
        vm.previous()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(1, state.currentIndex)
        assertEquals(false, state.isLastSlide)
    }

    @Test
    fun `previous on first slide does nothing`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.previous()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun `skip emits Completed with skipped true`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.skip()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Completed>(state)
        assertEquals(true, state.skipped)
    }

    @Test
    fun `start emits Completed with skipped false`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.start()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Completed>(state)
        assertEquals(false, state.skipped)
    }

    @Test
    fun `error from repository emits Error state`() = runTest(testDispatcher) {
        val vm = createViewModel(Result.failure(Exception("parse error")))
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Error>(state)
        assertEquals("parse error", state.message)
    }

    @Test
    fun `empty slides list auto-completes`() = runTest(testDispatcher) {
        val vm = createViewModel(Result.success(emptyList()))
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.state.value
        assertIs<OnboardingViewModel.OnboardingState.Completed>(state)
        assertEquals(true, state.skipped)
    }

    @Test
    fun `isLastSlide is true when navigating to last slide`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        vm.next()
        vm.next()
        val state = vm.state.value as OnboardingViewModel.OnboardingState.Loaded
        assertTrue(state.isLastSlide)
    }
}

private class FakeOnboardingRepository(
    private val result: Result<List<OnboardingSlide>>
) : OnboardingRepository {
    override suspend fun getSlides(): Result<List<OnboardingSlide>> = result
}

private class FakeOnboardingPreferences : OnboardingPreferences {
    override val isOnboardingCompleted: Flow<Boolean> = flowOf(false)
    override suspend fun setOnboardingCompleted() {
        // No-op for testing
    }
}