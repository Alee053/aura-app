package com.programovil.aura.todo.presentation.viewmodel

import app.cash.turbine.test
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import com.programovil.aura.todo.domain.usecase.UpdateTodoUseCase
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import io.mockative.of
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val getTodosUseCase = mock(of<GetTodosUseCase>())
    private val addTodoUseCase = mock(of<AddTodoUseCase>())
    private val updateTodoUseCase = mock(of<UpdateTodoUseCase>())
    private val toggleTodoUseCase = mock(of<ToggleTodoUseCase>())
    private val deleteTodoUseCase = mock(of<DeleteTodoUseCase>())

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getTodosUseCase() } returns flowOf(Result.success(emptyList()))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TodoViewModel = TodoViewModel(
        getTodosUseCase = getTodosUseCase,
        addTodoUseCase = addTodoUseCase,
        updateTodoUseCase = updateTodoUseCase,
        toggleTodoUseCase = toggleTodoUseCase,
        deleteTodoUseCase = deleteTodoUseCase
    )

    @Test
    fun `addTodo invokes use case with trimmed values`() = runTest(testDispatcher) {
        coEvery { addTodoUseCase("Buy milk", "Urgent", null) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.addTodo("Buy milk", "Urgent", null)

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { addTodoUseCase("Buy milk", "Urgent", null) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `updateTodo invokes use case`() = runTest(testDispatcher) {
        val todo = Todo(id = "1", title = "Updated", description = "Desc")
        coEvery { updateTodoUseCase(todo) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.updateTodo(todo)

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { updateTodoUseCase(todo) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `deleteTodo invokes use case`() = runTest(testDispatcher) {
        coEvery { deleteTodoUseCase("1") } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.deleteTodo("1")

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { deleteTodoUseCase("1") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `toggleTodo invokes use case`() = runTest(testDispatcher) {
        coEvery { toggleTodoUseCase("1", true) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.toggleTodo("1", true)

        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { toggleTodoUseCase("1", true) }.wasInvoked(exactly = 1)
    }
}