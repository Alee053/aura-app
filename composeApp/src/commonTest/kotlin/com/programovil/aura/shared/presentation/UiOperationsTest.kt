package com.programovil.aura.shared.presentation

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class UiOperationsTest {
    @Test fun pendingIsSynchronousAndDuplicateTapDoesNotWriteTwice() = runTest {
        val operations = UiOperations(backgroundScope)
        val completion = CompletableDeferred<Result<Unit>>()
        var calls = 0
        repeat(2) {
            operations.launch("item", "save", ErrorKey.TodoAdd) { calls++; completion.await() }
        }
        assertTrue(operations.isPending("item"))
        runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(10_001); runCurrent()
        assertTrue(operations.states.value.getValue("item").longRunning)
        completion.complete(Result.success(Unit)); runCurrent()
        val success = operations.states.value.getValue("item")
        assertEquals(OperationStatus.Succeeded, success.status)
        operations.consume("item", success.requestId + 1)
        assertNotNull(operations.states.value["item"])
        operations.consume("item", success.requestId)
        assertNull(operations.states.value["item"])
    }

    @Test fun failureRemainsUntilConsumedAndRetryHasANewIdentity() = runTest {
        val operations = UiOperations(backgroundScope)
        operations.launch("editor", "save", ErrorKey.TodoAdd) { throw IllegalStateException("offline") }
        runCurrent()
        val failed = operations.states.value.getValue("editor")
        assertEquals(OperationStatus.Failed, failed.status)
        assertEquals(ErrorKey.TodoAdd, failed.error)
        operations.launch("editor", "save", ErrorKey.TodoAdd) { Result.success(Unit) }
        runCurrent()
        val success = operations.states.value.getValue("editor")
        assertTrue(success.requestId > failed.requestId)
        operations.consume("editor", failed.requestId)
        assertEquals(success, operations.states.value["editor"])
    }

    @Test fun differentRowsCanProceedAndCompletedCleanupDoesNotCancelPending() = runTest {
        val operations = UiOperations(backgroundScope)
        operations.launch("a", "toggle", ErrorKey.TodoUpdate) { awaitCancellation() }
        operations.launch("b", "toggle", ErrorKey.TodoUpdate) { Result.success(Unit) }
        runCurrent()
        operations.clearCompleted()
        assertTrue(operations.isPending("a"))
        assertNull(operations.states.value["b"])
    }

    @Test fun cancellationIsNotReportedAsAnOperationFailure() = runTest {
        val job = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + job)
        val operations = UiOperations(scope)
        var reported = false
        operations.launch("editor", "save", ErrorKey.TodoAdd, { reported = true }) { awaitCancellation() }
        runCurrent(); job.cancel(); runCurrent()
        assertFalse(reported)
        assertNull(operations.states.value["editor"]?.error)
    }
}
