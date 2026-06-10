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

class DeleteTodoUseCaseTest {

    private val repository = mock(of<TodoRepository>())
    private val useCase = DeleteTodoUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        coEvery { repository.deleteTodo("t-1") } returns Result.success(Unit)

        val result = useCase("t-1")

        assertTrue(result.isSuccess)
        coVerify { repository.deleteTodo("t-1") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val error = RuntimeException("not found")
        coEvery { repository.deleteTodo("t-2") } returns Result.failure(error)

        val result = useCase("t-2")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
