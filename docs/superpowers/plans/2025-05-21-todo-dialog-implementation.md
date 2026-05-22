# Todo Dialog Creation/Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace inline todo creation with a reusable dialog that supports creating and editing todos, including title, optional description, and due date.

**Architecture:** A single `TodoDialog` composable handles both create and edit modes. The dialog is opened from the FAB (create) or by tapping a todo card (edit). Data layer gains an `updateTodo` operation; `addTodo` gains a `description` parameter. All strings externalized.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Koin DI, Mockative + Turbine for tests, Firebase Firestore (Android), DataStore for preferences.

---

## File Structure

| File | Action | Responsibility |
|------|--------|--------------|
| `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/model/Todo.kt` | Modify | Add `description` field |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/mapper/TodoMapper.kt` | Modify | Add `description` to DTO and mapper |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepository.kt` | Modify | Add `updateTodo`; update `addTodo` signature |
| `composeApp/src/androidMain/.../todo/data/repository/TodoRepositoryImpl.kt` | Modify | Implement `updateTodo`; map `description` |
| `composeApp/src/iosMain/.../todo/data/repository/TodoRepositoryImpl.kt` | Modify | Stub `updateTodo`; update `addTodo` signature |
| `composeApp/src/commonMain/.../todo/domain/usecase/AddTodoUseCase.kt` | Modify | Pass `description` through |
| `composeApp/src/commonMain/.../todo/domain/usecase/UpdateTodoUseCase.kt` | Create | New use case for updating todos |
| `composeApp/src/commonMain/.../todo/presentation/viewmodel/TodoViewModel.kt` | Modify | Add `updateTodo`; update `addTodo` |
| `composeApp/src/commonMain/.../todo/di/TodoModule.kt` | Modify | Register `UpdateTodoUseCase`; update ViewModel injection |
| `composeApp/src/commonMain/.../todo/presentation/composable/TodoDialog.kt` | Create | Reusable create/edit dialog |
| `composeApp/src/commonMain/.../todo/presentation/composable/TodoItem.kt` | Modify | Add `onClick`; show description; remove delete icon |
| `composeApp/src/commonMain/.../todo/presentation/screen/TodoScreen.kt` | Modify | Remove inline input; wire FAB and empty state to dialog |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | Modify | Add todo dialog strings |
| `composeApp/src/commonMain/composeResources/values-es/strings.xml` | Modify | Add Spanish translations |
| `composeApp/src/commonTest/.../todo/domain/usecase/AddTodoUseCaseTest.kt` | Modify | Update for `description` param |
| `composeApp/src/commonTest/.../todo/domain/usecase/UpdateTodoUseCaseTest.kt` | Create | Test new use case |
| `composeApp/src/commonTest/.../todo/presentation/viewmodel/TodoViewModelTest.kt` | Create | Test ViewModel with new operations |

---

### Task 1: Update Todo domain model and TodoData mapper

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/model/Todo.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/mapper/TodoMapper.kt`

- [ ] **Step 1: Add `description` to `Todo` data class**

```kotlin
package com.programovil.aura.todo.domain.model

data class Todo(
    val id: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null
)
```

- [ ] **Step 2: Update `TodoData` and mapper**

```kotlin
package com.programovil.aura.todo.data.mapper

import com.programovil.aura.todo.domain.model.Todo

data class TodoData(
    val id: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean,
    val dueDate: Long? = null
)

fun TodoData.toDomain(): Todo = Todo(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    dueDate = dueDate
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/model/Todo.kt \
    composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/mapper/TodoMapper.kt
git commit -m "feat(todo): add optional description field to Todo model and mapper"
```

---

### Task 2: Update TodoRepository interface

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepository.kt`

- [ ] **Step 1: Add `updateTodo` and update `addTodo` signature**

```kotlin
package com.programovil.aura.todo.domain.repository

import com.programovil.aura.todo.domain.model.Todo
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String, description: String? = null, dueDate: Long? = null): Result<Unit>
    suspend fun updateTodo(todo: Todo): Result<Unit>
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}

expect fun createTodoRepository(): TodoRepository
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepository.kt
git commit -m "feat(todo): add updateTodo to repository interface and description param to addTodo"
```

---

### Task 3: Update Android TodoRepositoryImpl

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt`

- [ ] **Step 1: Map `description` in reads and writes; add `updateTodo`**

```kotlin
package com.programovil.aura.todo.domain.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.todo.data.mapper.TodoData
import com.programovil.aura.todo.data.mapper.toDomain
import com.programovil.aura.todo.domain.model.Todo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual fun createTodoRepository(): TodoRepository = TodoRepositoryImpl()

private class TodoRepositoryImpl : TodoRepository {

    private val userId: String
        get() = FirebaseConfig.auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userTodosCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("todos")

    override fun getTodos(): Flow<Result<List<Todo>>> = callbackFlow {
        val listener = userTodosCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val todos = snapshot?.documents?.mapNotNull { doc ->
                    val todoData = TodoData(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description"),
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        dueDate = doc.getLong("dueDate")
                    )
                    todoData.toDomain()
                } ?: emptyList<Todo>()
                trySend(Result.success(todos))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addTodo(title: String, description: String?, dueDate: Long?): Result<Unit> = runCatching {
        val data = mutableMapOf(
            "title" to title,
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (description != null) {
            data["description"] = description
        }
        if (dueDate != null) {
            data["dueDate"] = dueDate
        }
        userTodosCollection().add(data).await()
    }

    override suspend fun updateTodo(todo: Todo): Result<Unit> = runCatching {
        val data = mutableMapOf<String, Any>(
            "title" to todo.title,
            "isCompleted" to todo.isCompleted
        )
        if (todo.description != null) {
            data["description"] = todo.description
        } else {
            data["description"] = com.google.firebase.firestore.FieldValue.delete()
        }
        if (todo.dueDate != null) {
            data["dueDate"] = todo.dueDate
        } else {
            data["dueDate"] = com.google.firebase.firestore.FieldValue.delete()
        }
        userTodosCollection().document(todo.id).update(data).await()
    }

    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit> = runCatching {
        userTodosCollection().document(todoId)
            .update("isCompleted", isCompleted).await()
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> = runCatching {
        userTodosCollection().document(todoId).delete().await()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt
git commit -m "feat(todo): implement updateTodo and description support in Android repository"
```

---

### Task 4: Update iOS TodoRepositoryImpl stub

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt`

- [ ] **Step 1: Update stub signatures**

```kotlin
package com.programovil.aura.todo.domain.repository

import com.programovil.aura.todo.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createTodoRepository(): TodoRepository = IosTodoRepositoryImpl()

private class IosTodoRepositoryImpl : TodoRepository {
    override fun getTodos(): Flow<Result<List<Todo>>> = flowOf(Result.success(emptyList()))
    override suspend fun addTodo(title: String, description: String?, dueDate: Long?): Result<Unit> = Result.success(Unit)
    override suspend fun updateTodo(todo: Todo): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTodo(todoId: String): Result<Unit> = Result.success(Unit)
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt
git commit -m "feat(todo): stub updateTodo and description in iOS repository"
```

---

### Task 5: Update AddTodoUseCase and create UpdateTodoUseCase

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/AddTodoUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/UpdateTodoUseCase.kt`

- [ ] **Step 1: Update `AddTodoUseCase` to accept `description`**

```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class AddTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        dueDate: Long? = null
    ): Result<Unit> = repository.addTodo(title, description, dueDate)
}
```

- [ ] **Step 2: Create `UpdateTodoUseCase`**

```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository

class UpdateTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todo: Todo): Result<Unit> = repository.updateTodo(todo)
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/AddTodoUseCase.kt \
    composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/UpdateTodoUseCase.kt
git commit -m "feat(todo): add description to AddTodoUseCase and create UpdateTodoUseCase"
```

---

### Task 6: Update TodoViewModel and DI module

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/di/TodoModule.kt`

- [ ] **Step 1: Update `TodoViewModel`**

```kotlin
package com.programovil.aura.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import com.programovil.aura.todo.domain.usecase.UpdateTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoViewModel(
    private val getTodosUseCase: GetTodosUseCase,
    private val addTodoUseCase: AddTodoUseCase,
    private val updateTodoUseCase: UpdateTodoUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTodoUseCase: DeleteTodoUseCase
) : ViewModel() {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    val todos: StateFlow<List<Todo>> = _todos
    val error: StateFlow<String?> = _error
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            getTodosUseCase().collect { result ->
                result.onSuccess { _todos.value = it }
                    .onFailure { _error.value = it.message ?: "Unknown error" }
                _isLoading.value = false
            }
        }
    }

    fun addTodo(title: String, description: String? = null, dueDate: Long? = null) {
        if (title.isBlank()) return
        _error.value = null
        viewModelScope.launch {
            addTodoUseCase(title.trim(), description?.trim(), dueDate)
                .onFailure { _error.value = "Failed to add todo" }
        }
    }

    fun updateTodo(todo: Todo) {
        _error.value = null
        viewModelScope.launch {
            updateTodoUseCase(todo)
                .onFailure { _error.value = "Failed to update todo" }
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        _error.value = null
        viewModelScope.launch {
            toggleTodoUseCase(todoId, isCompleted)
                .onFailure { _error.value = "Failed to update todo" }
        }
    }

    fun deleteTodo(todoId: String) {
        _error.value = null
        viewModelScope.launch {
            deleteTodoUseCase(todoId)
                .onFailure { _error.value = "Failed to delete todo" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
```

- [ ] **Step 2: Update `TodoModule`**

```kotlin
package com.programovil.aura.todo.di

import com.programovil.aura.todo.domain.repository.TodoRepository
import com.programovil.aura.todo.domain.repository.createTodoRepository
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import com.programovil.aura.todo.domain.usecase.UpdateTodoUseCase
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val todoModule = module {
    // Data layer
    single { createTodoRepository() }

    // Domain layer - use cases
    factoryOf(::GetTodosUseCase)
    factoryOf(::AddTodoUseCase)
    factoryOf(::UpdateTodoUseCase)
    factoryOf(::ToggleTodoUseCase)
    factoryOf(::DeleteTodoUseCase)

    // Presentation layer
    viewModel { TodoViewModel(get(), get(), get(), get(), get()) }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModel.kt \
    composeApp/src/commonMain/kotlin/com/programovil/aura/todo/di/TodoModule.kt
git commit -m "feat(todo): add updateTodo to ViewModel and register UpdateTodoUseCase in DI"
```

---

### Task 7: Add string resources

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-es/strings.xml`

- [ ] **Step 1: Add English strings**

Insert the following entries into `composeApp/src/commonMain/composeResources/values/strings.xml` inside the `<resources>` block (after the existing Todo Screen section, before the Habit Screen section):

```xml
    <!-- Todo Dialog -->
    <string name="new_todo">New Todo</string>
    <string name="edit_todo">Edit Todo</string>
    <string name="todo_name_hint">Todo name</string>
    <string name="todo_description_hint">Description (optional)</string>
    <string name="add_due_date">Add due date</string>
    <string name="clear_due_date">Clear</string>
    <string name="add_first_todo">Add first todo</string>
```

Also update `add_todo` from `+` to a more meaningful value (used for FAB content description):
```xml
    <string name="add_todo">Add todo</string>
```

- [ ] **Step 2: Add Spanish translations**

Insert the following into `composeApp/src/commonMain/composeResources/values-es/strings.xml` in the corresponding location:

```xml
    <!-- Todo Dialog -->
    <string name="new_todo">Nueva tarea</string>
    <string name="edit_todo">Editar tarea</string>
    <string name="todo_name_hint">Nombre de la tarea</string>
    <string name="todo_description_hint">Descripción (opcional)</string>
    <string name="add_due_date">Agregar fecha</string>
    <string name="clear_due_date">Borrar</string>
    <string name="add_first_todo">Agregar primera tarea</string>
```

And update `add_todo`:
```xml
    <string name="add_todo">Agregar tarea</string>
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/composeResources/values/strings.xml \
    composeApp/src/commonMain/composeResources/values-es/strings.xml
git commit -m "feat(todo): add todo dialog string resources with Spanish translations"
```

---

### Task 8: Create TodoDialog composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoDialog.kt`

- [ ] **Step 1: Write `TodoDialog` composable**

```kotlin
package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.domain.model.Todo
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.add_due_date
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.clear_due_date
import aura_app.composeapp.generated.resources.edit_todo
import aura_app.composeapp.generated.resources.new_todo
import aura_app.composeapp.generated.resources.save
import aura_app.composeapp.generated.resources.todo_description_hint
import aura_app.composeapp.generated.resources.todo_name_hint
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDialog(
    todo: Todo?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, dueDate: Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(todo?.title ?: "") }
    var description by remember { mutableStateOf(todo?.description ?: "") }
    var dueDate by remember { mutableStateOf(todo?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEditMode = todo != null

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(if (isEditMode) Res.string.edit_todo else Res.string.new_todo),
                    style = AppTheme.typography.headlineSmall,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                BasicInput(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(Res.string.todo_name_hint),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.todo_description_hint)) },
                    singleLine = false,
                    maxLines = 3,
                    textStyle = AppTheme.typography.bodyMedium.copy(
                        color = AppTheme.colors.textPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTheme.colors.textPrimary,
                        unfocusedTextColor = AppTheme.colors.textPrimary,
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.textPrimary.copy(alpha = 0.5f),
                        focusedLabelColor = AppTheme.colors.primary,
                        unfocusedLabelColor = AppTheme.colors.textSecondary,
                        cursorColor = AppTheme.colors.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dateText = dueDate?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        date.toString()
                    } ?: stringResource(Res.string.add_due_date)

                    Text(
                        text = dateText,
                        style = AppTheme.typography.bodyMedium,
                        color = if (dueDate != null) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                    )

                    if (dueDate != null) {
                        IconButton(
                            onClick = { dueDate = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.clear_due_date),
                                tint = AppTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditMode && onDelete != null) {
                        TextButton(onClick = {
                            onDelete()
                            onDismiss()
                        }) {
                            Text(
                                text = "Delete",
                                style = AppTheme.typography.labelLarge,
                                color = AppTheme.colors.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onDismiss
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    PrimaryButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title.trim(),
                                    description.takeIf { it.isNotBlank() }?.trim(),
                                    dueDate
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank()
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoDialog.kt
git commit -m "feat(todo): add reusable TodoDialog for create and edit modes"
```

---

### Task 9: Update TodoItem composable

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt`

- [ ] **Step 1: Add `onClick`, show description, remove delete icon**

```kotlin
package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.domain.model.Todo
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.due_label
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AppTheme.colors.primary,
                    uncheckedColor = AppTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                todo.description?.let { desc ->
                    Text(
                        text = desc,
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                todo.dueDate?.let { millis ->
                    val date = Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    Text(
                        text = stringResource(Res.string.due_label, date.toString()),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt
git commit -m "feat(todo): make TodoItem clickable, show description, remove inline delete"
```

---

### Task 10: Update TodoScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Rewrite TodoScreen with dialog-based creation/editing**

```kotlin
package com.programovil.aura.todo.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.presentation.composable.TodoDialog
import com.programovil.aura.todo.presentation.composable.TodoItem
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.add_first_todo
import aura_app.composeapp.generated.resources.add_todo
import aura_app.composeapp.generated.resources.empty_todos
import aura_app.composeapp.generated.resources.todos_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel
) {
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDialog) {
        TodoDialog(
            todo = editingTodo,
            onDismiss = {
                showDialog = false
                editingTodo = null
            },
            onSave = { title, description, dueDate ->
                if (editingTodo != null) {
                    viewModel.updateTodo(
                        editingTodo!!.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate
                        )
                    )
                } else {
                    viewModel.addTodo(title, description, dueDate)
                }
            },
            onDelete = editingTodo?.let { todo ->
                {
                    viewModel.deleteTodo(todo.id)
                }
            }
        )
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.todos_title),
                        style = AppTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTodo = null
                    showDialog = true
                },
                containerColor = AppTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.add_todo),
                    tint = AppTheme.colors.textPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }
                todos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(Res.string.empty_todos),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary
                            )
                            PrimaryButton(
                                text = stringResource(Res.string.add_first_todo),
                                onClick = {
                                    editingTodo = null
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(todos, key = { it.id }) { todo ->
                            TodoItem(
                                todo = todo,
                                onToggle = { viewModel.toggleTodo(todo.id, !todo.isCompleted) },
                                onClick = {
                                    editingTodo = todo
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt
git commit -m "feat(todo): replace inline input with TodoDialog for create and edit"
```

---

### Task 11: Update tests

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/programovil/aura/todo/domain/usecase/AddTodoUseCaseTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/todo/domain/usecase/UpdateTodoUseCaseTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModelTest.kt`

- [ ] **Step 1: Update `AddTodoUseCaseTest`**

```kotlin
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
        coEvery { repository.addTodo("Buy milk", null, null) } returns Result.success(Unit)

        val result = useCase("Buy milk")

        assertTrue(result.isSuccess)
        coVerify { repository.addTodo("Buy milk", null, null) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke forwards due date and description to repository`() = runTest {
        val dueDate = 1_700_000_000_000L
        coEvery { repository.addTodo("Meeting", "Discuss Q1", dueDate) } returns Result.success(Unit)

        val result = useCase("Meeting", "Discuss Q1", dueDate)

        assertTrue(result.isSuccess)
        coVerify { repository.addTodo("Meeting", "Discuss Q1", dueDate) }.wasInvoked(exactly = 1)
    }
}
```

- [ ] **Step 2: Create `UpdateTodoUseCaseTest`**

```kotlin
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
```

- [ ] **Step 3: Create `TodoViewModelTest`**

```kotlin
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
```

- [ ] **Step 4: Run unit tests**

```bash
./gradlew :composeApp:testDebugUnitTest
```

Expected: All tests pass (including `AddTodoUseCaseTest`, `GetTodosUseCaseTest`, `UpdateTodoUseCaseTest`, `TodoViewModelTest`).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/todo/domain/usecase/AddTodoUseCaseTest.kt \
    composeApp/src/commonTest/kotlin/com/programovil/aura/todo/domain/usecase/UpdateTodoUseCaseTest.kt \
    composeApp/src/commonTest/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModelTest.kt
git commit -m "test(todo): update AddTodoUseCaseTest and add UpdateTodoUseCaseTest and TodoViewModelTest"
```

---

### Task 12: Build verification

- [ ] **Step 1: Run Android unit tests**

```bash
./gradlew :composeApp:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL` with no compilation errors.

- [ ] **Step 3: Check for unused imports or lint issues**

```bash
./gradlew :composeApp:lintDebug
```

If there are lint issues, fix them in place (typically unused imports in `TodoScreen.kt` or `TodoDialog.kt`).

- [ ] **Step 4: Commit if clean**

```bash
git status
git commit -m "build: verify all tests pass and compilation clean after todo dialog refactor" || echo "Nothing to commit"
```

---

## Self-Review

### Spec coverage check

| Spec requirement | Task that implements it |
|------------------|------------------------|
| Add `description` to `Todo` model | Task 1 |
| Update `TodoData` mapper | Task 1 |
| Add `updateTodo` to repository interface | Task 2 |
| Update `addTodo` signature with `description` | Task 2 |
| Android impl: `updateTodo` + `description` read/write | Task 3 |
| iOS stub: `updateTodo` + updated signatures | Task 4 |
| Create `UpdateTodoUseCase` | Task 5 |
| Update `AddTodoUseCase` | Task 5 |
| Update `TodoViewModel` with `updateTodo` | Task 6 |
| Update DI module | Task 6 |
| Add string resources (en + es) | Task 7 |
| Create `TodoDialog` | Task 8 |
| Update `TodoItem` (clickable, description, no delete) | Task 9 |
| Update `TodoScreen` (remove inline input, wire dialog) | Task 10 |
| Update tests | Task 11 |
| Build verification | Task 12 |

### Placeholder scan

- No "TBD", "TODO", or "implement later" found.
- No vague instructions like "add appropriate error handling".
- Every task shows exact code or exact commands.
- Type names are consistent across all tasks (`Todo`, `TodoData`, `UpdateTodoUseCase`, etc.).

### Type consistency check

- `TodoRepository.addTodo` signature: `(String, String?, Long?)` — consistent in interface, Android, iOS, use case, ViewModel.
- `TodoRepository.updateTodo` signature: `(Todo)` — consistent everywhere.
- `Todo` fields: `id`, `title`, `description`, `isCompleted`, `dueDate` — consistent in model, mapper, repository read, dialog state.
- `TodoViewModel` constructor: 5 parameters in definition, 5 in DI module, 5 in test setup.

Plan is complete and ready for execution.
