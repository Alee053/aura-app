package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToggleTodoUseCaseTest {

    private val repository = mock(of<TodoRepository>())
    private val useCase = ToggleTodoUseCase(repository)

    @Test
    fun `invoke forwards id and isCompleted to repository`() = runTest {
        coEvery { repository.toggleTodo("t-1", true) } returns Result.success(Unit)

        val result = useCase("t-1", true)

        assertTrue(result.isSuccess)
        coVerify { repository.toggleTodo("t-1", true) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke returns success when marking as not completed`() = runTest {
        coEvery { repository.toggleTodo("t-2", false) } returns Result.success(Unit)

        val result = useCase("t-2", false)

        assertTrue(result.isSuccess)
        coVerify { repository.toggleTodo("t-2", false) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val error = RuntimeException("network down")
        coEvery { repository.toggleTodo("t-3", true) } returns Result.failure(error)

        val result = useCase("t-3", true)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
