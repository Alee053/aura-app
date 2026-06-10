package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.programovil.aura.experiments.domain.model.HomeVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.usecase.GetHomeVariantUseCase
import com.programovil.aura.experiments.domain.usecase.GetMotivationPhraseUseCase
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.usecase.GetHabitsWithStatusUseCase
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.home.domain.usecase.GetDashboardDataUseCase
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getTodosUseCase = mock(of<GetTodosUseCase>())
    private val getHabitsWithStatusUseCase = mock(of<GetHabitsWithStatusUseCase>())
    private val getUserPlanUseCase = mock(of<GetUserPlanUseCase>())
    private val getMotivationPhraseUseCase = mock(of<GetMotivationPhraseUseCase>())
    private val viewModels = mutableListOf<HomeViewModel>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with empty data and no variant`() = runTest(testDispatcher) {
        stubEmptySources()

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(DashboardData(), viewModel.uiState.value.dashboardData)
        assertEquals("en", viewModel.uiState.value.locale)
    }

    @Test
    fun `successful dashboard emission clears loading and updates data`() = runTest(testDispatcher) {
        val todos = listOf(
            Todo("t-1", "A", null, isCompleted = false, dueDate = null),
            Todo("t-2", "B", null, isCompleted = true, dueDate = null)
        )
        val habits = listOf(
            HabitWithStatus(
                habit = habit("h-1", "Run"),
                currentPeriodProgress = 1 to 1,
                streak = 5,
                last7Days = listOf(DayCompletion("2026-06-10", isCompleted = true))
            )
        )
        every { getTodosUseCase() } returns flowOf(Result.success(todos))
        every { getHabitsWithStatusUseCase() } returns flowOf(Result.success(habits))
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Free)
        every { getMotivationPhraseUseCase() } returns flowOf(emptyMap())

        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.dashboardData.incompleteTodos)
        assertEquals(1, state.dashboardData.completedHabitsToday)
        assertEquals(1, state.dashboardData.totalHabitsToday)
        assertEquals(5, state.dashboardData.currentStreak)
    }

    @Test
    fun `failed dashboard emission clears loading but keeps empty data`() = runTest(testDispatcher) {
        every { getTodosUseCase() } returns flowOf(Result.failure(RuntimeException("boom")))
        every { getHabitsWithStatusUseCase() } returns flowOf(Result.failure(RuntimeException("boom")))
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Free)
        every { getMotivationPhraseUseCase() } returns flowOf(emptyMap())

        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(DashboardData(), state.dashboardData)
    }

    @Test
    fun `home variant emission updates the state`() = runTest(testDispatcher) {
        stubEmptySources()
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Premium)
        every { getMotivationPhraseUseCase() } returns flowOf(mapOf("en" to "Hello", "es" to "Hola"))

        val viewModel = createViewModel()
        runCurrent()

        val variantValue = HomeVariant(
            showsDailyMotivation = true,
            tone = Tone.Direct,
            motivationPhrase = mapOf("en" to "Hello", "es" to "Hola")
        )
        assertEquals(variantValue, viewModel.uiState.value.homeVariant)
    }

    @Test
    fun `resolvedMotivationPhrase prefers current locale then en then empty`() = runTest(testDispatcher) {
        stubEmptySources()
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Premium)
        every { getMotivationPhraseUseCase() } returns flowOf(mapOf("en" to "Hello", "es" to "Hola"))

        val viewModel = createViewModel(locale = "es")
        runCurrent()

        assertEquals("Hola", viewModel.uiState.value.resolvedMotivationPhrase)
    }

    @Test
    fun `resolvedMotivationPhrase falls back to en when current locale is missing`() = runTest(testDispatcher) {
        stubEmptySources()
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Premium)
        every { getMotivationPhraseUseCase() } returns flowOf(mapOf("en" to "Hello"))

        val viewModel = createViewModel(locale = "fr")
        runCurrent()

        assertEquals("Hello", viewModel.uiState.value.resolvedMotivationPhrase)
    }

    @Test
    fun `resolvedMotivationPhrase is empty when neither locale is in the map`() = runTest(testDispatcher) {
        stubEmptySources()
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Premium)
        every { getMotivationPhraseUseCase() } returns flowOf(emptyMap())

        val viewModel = createViewModel(locale = "en")
        runCurrent()

        assertEquals("", viewModel.uiState.value.resolvedMotivationPhrase)
    }

    private fun stubEmptySources() {
        every { getTodosUseCase() } returns flowOf(Result.success(emptyList()))
        every { getHabitsWithStatusUseCase() } returns flowOf(Result.success(emptyList()))
        every { getUserPlanUseCase() } returns flowOf(com.programovil.aura.experiments.domain.model.UserPlan.Free)
        every { getMotivationPhraseUseCase() } returns flowOf(emptyMap())
    }

    private fun createViewModel(locale: String = "en"): HomeViewModel {
        val dashboard = GetDashboardDataUseCase(getTodosUseCase, getHabitsWithStatusUseCase)
        val variant = GetHomeVariantUseCase(getUserPlanUseCase, getMotivationPhraseUseCase)
        val viewModel = HomeViewModel(dashboard, variant, locale = locale)
        viewModels += viewModel
        return viewModel
    }

    private fun habit(id: String, name: String) = Habit(
        id = id,
        name = name,
        recurrenceType = RecurrenceType.DAILY,
        targetCount = 1,
        color = "#000000",
        createdAt = 0L
    )
}
