package com.programovil.aura.shared.presentation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class OperationStatus { Pending, Succeeded, Failed }
data class UiOperation(
    val requestId: Long, val target: String, val kind: String,
    val status: OperationStatus, val error: UiText? = null, val longRunning: Boolean = false
) {
    val pending get() = status == OperationStatus.Pending
}
class UiOperations(private val scope: CoroutineScope) {
    private var sequence = 0L
    private val _states = MutableStateFlow<Map<String, UiOperation>>(emptyMap())
    val states = _states.asStateFlow()
    fun isPending(key: String) = _states.value[key]?.pending == true
    fun consume(key: String, requestId: Long) {
        if (_states.value[key]?.requestId == requestId && !isPending(key))
            _states.update { it - key }
    }
    fun clearCompleted() { _states.update { states -> states.filterValues { it.pending } } }
    fun launch(
        key: String, kind: String, error: UiText,
        onResult: (Result<Unit>) -> Unit = {},
        action: suspend () -> Result<Unit>
    ) {
        if (isPending(key)) return
        val request = UiOperation(++sequence, key, kind, OperationStatus.Pending)
        _states.update { it + (key to request) }
        scope.launch {
            val slow = launch {
                delay(10_000)
                _states.update { it + (key to request.copy(longRunning = true)) }
            }
            val result = try { action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { Result.failure(failure) }
            finally { slow.cancel() }
            onResult(result)
            _states.update { it + (key to request.copy(
                status = if (result.isSuccess) OperationStatus.Succeeded else OperationStatus.Failed,
                error = if (result.isFailure) error else null
            )) }
        }
    }
}
