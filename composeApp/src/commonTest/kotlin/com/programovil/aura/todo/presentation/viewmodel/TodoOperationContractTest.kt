package com.programovil.aura.todo.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import com.programovil.aura.todo.domain.usecase.*
import com.programovil.aura.shared.presentation.OperationStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TodoOperationContractTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }
    private fun model(repo: PendingTodoRepository) = TodoViewModel(GetTodosUseCase(repo), AddTodoUseCase(repo),
        UpdateTodoUseCase(repo), ToggleTodoUseCase(repo), DeleteTodoUseCase(repo))

    @Test fun optimisticToggleDoesNotMoveTheRowBeforeConfirmation() = runTest(dispatcher) {
        val repo = PendingTodoRepository()
        val vm = model(repo)
        try {
            runCurrent()
            vm.toggleTodo("t1", true)
            runCurrent()
            repo.rows.value = Result.success(listOf(repo.original.copy(isCompleted = true)))
            runCurrent()
            assertFalse(vm.todos.value.single().isCompleted)
            assertTrue(vm.operations.isPending("toggle:t1"))
            repo.result.complete(Result.success(Unit)); runCurrent()
            assertTrue(vm.todos.value.single().isCompleted)
            assertEquals(OperationStatus.Succeeded, vm.operations.states.value["toggle:t1"]?.status)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun optimisticDeleteRemainsAccessibleWhenTheWriteFails() = runTest(dispatcher) {
        val repo = PendingTodoRepository()
        val vm = model(repo)
        try {
            runCurrent()
            vm.deleteTodo("t1"); runCurrent()
            repo.rows.value = Result.success(emptyList()); runCurrent()
            assertEquals(repo.original, vm.todos.value.single())
            repo.result.complete(Result.failure(IllegalStateException("offline"))); runCurrent()
            assertEquals(repo.original, vm.todos.value.single())
            assertEquals(OperationStatus.Failed, vm.operations.states.value["editor"]?.status)
        } finally { vm.viewModelScope.cancel() }
    }
}

private class PendingTodoRepository : TodoRepository {
    val original = Todo("t1", "Keep my task", null, false, null)
    val rows = MutableStateFlow(Result.success(listOf(original)))
    val result = CompletableDeferred<Result<Unit>>()
    override fun getTodos() = rows
    override suspend fun addTodo(title: String, description: String?, dueDate: Long?) = result.await()
    override suspend fun updateTodo(todo: Todo) = result.await()
    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean) = result.await()
    override suspend fun deleteTodo(todoId: String) = result.await()
}
