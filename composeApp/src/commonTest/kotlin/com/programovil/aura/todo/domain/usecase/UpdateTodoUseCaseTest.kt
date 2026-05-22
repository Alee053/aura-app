package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UpdateTodoUseCaseTest {

    private val repository = mock(of<TodoRepository>())
    private val useCase = UpdateTodoUseCase(repository)

    @Test
    fun `invoke updates todo successfully`() = runTest {
        val todo = Todo(
            id = "1",
            title = "Updated title",
            description = "Updated desc",
            isCompleted = false,
            dueDate = 1_700_000_000_000L
        )
        coEvery { repository.updateTodo(todo) } returns Result.success(Unit)

        val result = useCase(todo)

        assertTrue(result.isSuccess)
        coVerify { repository.updateTodo(todo) }.wasInvoked(exactly = 1)
    }
}