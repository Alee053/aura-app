package com.programovil.aura.habit.presentation.viewmodel

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.DeleteHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.domain.usecase.UpdateHabitUseCase
import io.mockative.classOf
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {

    private val repository = mock(classOf<HabitRepository>())
    private val getHabitsGroupedByDayUseCase = mock(classOf<GetHabitsGroupedByDayUseCase>())
    private val addHabitUseCase = mock(classOf<AddHabitUseCase>())
    private val updateHabitUseCase = mock(classOf<UpdateHabitUseCase>())
    private val deleteHabitUseCase = mock(classOf<DeleteHabitUseCase>())
    private val toggleHabitCompletionUseCase = mock(classOf<ToggleHabitCompletionUseCase>())
    private val getHabitHistoryUseCase = mock(classOf<GetHabitHistoryUseCase>())

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getHabitsGroupedByDayUseCase.invoke() } returns flowOf(Result.success(emptyMap()))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HabitViewModel = HabitViewModel(
        repository = repository,
        getHabitsGroupedByDayUseCase = getHabitsGroupedByDayUseCase,
        addHabitUseCase = addHabitUseCase,
        updateHabitUseCase = updateHabitUseCase,
        deleteHabitUseCase = deleteHabitUseCase,
        toggleHabitCompletionUseCase = toggleHabitCompletionUseCase,
        getHabitHistoryUseCase = getHabitHistoryUseCase
    )

    @Test
    fun `addHabit invokes use case with correct parameters`() = runTest(testDispatcher) {
        coEvery { addHabitUseCase("Exercise", RecurrenceType.DAILY, emptyList(), "#FF6B6B") } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.onEvent(HabitEvent.AddHabit("Exercise", RecurrenceType.DAILY, emptyList(), "#FF6B6B"))

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { addHabitUseCase("Exercise", RecurrenceType.DAILY, emptyList(), "#FF6B6B") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `updateHabit invokes use case`() = runTest(testDispatcher) {
        val habit = Habit(
            id = "h1",
            name = "Exercise",
            recurrenceType = RecurrenceType.DAILY,
            daysOfWeek = emptyList(),
            color = "#FF6B6B"
        )
        coEvery { updateHabitUseCase(habit) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.onEvent(HabitEvent.UpdateHabit(habit))

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { updateHabitUseCase(habit) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `deleteHabit invokes use case`() = runTest(testDispatcher) {
        coEvery { deleteHabitUseCase("h1") } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.onEvent(HabitEvent.DeleteHabit("h1"))

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { deleteHabitUseCase("h1") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `toggleCompletion invokes use case`() = runTest(testDispatcher) {
        coEvery { toggleHabitCompletionUseCase("h1", "2024-01-15") } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.onEvent(HabitEvent.ToggleCompletion("h1", "2024-01-15"))

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { toggleHabitCompletionUseCase("h1", "2024-01-15") }.wasInvoked(exactly = 1)
    }
}