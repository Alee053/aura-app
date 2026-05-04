package com.programovil.aura.todo.domain.usecase

import app.cash.turbine.test
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetTodosUseCaseTest {

    private val repository = mock(of<TodoRepository>())
    private val useCase = GetTodosUseCase(repository)

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val todos = listOf(
            Todo(id = "1", title = "Buy milk", isCompleted = false),
            Todo(id = "2", title = "Walk dog", isCompleted = true)
        )
        every { repository.getTodos() } returns flowOf(Result.success(todos))

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(todos, result.getOrNull())
            awaitComplete()
        }
    }
}
