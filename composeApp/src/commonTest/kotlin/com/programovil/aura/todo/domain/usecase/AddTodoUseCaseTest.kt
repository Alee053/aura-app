package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class AddTodoUseCaseTest {

    private val repository = mock(of<TodoRepository>())
    private val useCase = AddTodoUseCase(repository)

    @Test
    fun `invoke adds todo successfully`() = runTest {
        coEvery { repository.addTodo("Buy milk", null) } returns Result.success(Unit)

        val result = useCase("Buy milk")

        assertTrue(result.isSuccess)
        coVerify { repository.addTodo("Buy milk", null) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke forwards due date to repository`() = runTest {
        val dueDate = 1_700_000_000_000L
        coEvery { repository.addTodo("Meeting", dueDate) } returns Result.success(Unit)

        val result = useCase("Meeting", dueDate)

        assertTrue(result.isSuccess)
        coVerify { repository.addTodo("Meeting", dueDate) }.wasInvoked(exactly = 1)
    }
}
