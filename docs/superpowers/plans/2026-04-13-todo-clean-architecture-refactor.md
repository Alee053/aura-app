# Todo Clean Architecture Refactor Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the `todo` feature to follow Clean Architecture with proper data/domain/presentation separation, move navigation to root level, and consolidate DI into global modules.

**Architecture:** Feature-based Clean Architecture with domain/data/presentation layers. Each feature has its own DI module. Global DI modules aggregate feature modules.

**Tech Stack:** Kotlin, Jetpack Compose, Koin, Firebase Firestore

---

## File Map

### New Files to Create
- `navigation/NavRoute.kt` (move from `presentation/navigation/`)
- `navigation/AppNavHost.kt` (move from `presentation/navigation/`)
- `di/DataModule.kt`
- `di/DomainModule.kt`
- `di/PresentationModule.kt`
- `di/InitKoin.kt`
- `todo/domain/usecase/GetTodosUseCase.kt`
- `todo/domain/usecase/AddTodoUseCase.kt`
- `todo/domain/usecase/ToggleTodoUseCase.kt`
- `todo/domain/usecase/DeleteTodoUseCase.kt`
- `todo/data/mapper/TodoMapper.kt`
- `todo/presentation/screen/TodoScreen.kt`
- `todo/presentation/viewmodel/TodoViewModel.kt`
- `todo/presentation/composable/TodoItem.kt`

### Files to Delete
- `presentation/navigation/NavRoute.kt`
- `presentation/navigation/AppNavHost.kt`

### Files to Modify
- `App.kt`
- `todo/di/TodoModule.kt`
- `todo/data/repository/TodoRepositoryImpl.kt` (update imports)
- `di/AppModules.kt` (replace with InitKoin.kt + module files)

---

## Phase 1: Create Global DI Modules

### Task 1: Create Global DI Module Structure

**Files:**
- Create: `di/DataModule.kt`
- Create: `di/DomainModule.kt`
- Create: `di/PresentationModule.kt`
- Create: `di/InitKoin.kt`

- [ ] **Step 1: Create DataModule.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/di/DataModule.kt`:
```kotlin
package com.programovil.aura.di

import com.programovil.aura.todo.data.repository.TodoRepositoryImpl
import com.programovil.aura.todo.domain.repository.TodoRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    singleOf(::TodoRepositoryImpl).bind<TodoRepository>()
}
```

- [ ] **Step 2: Create DomainModule.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/di/DomainModule.kt`:
```kotlin
package com.programovil.aura.di

import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::GetTodosUseCase)
    factoryOf(::AddTodoUseCase)
    factoryOf(::ToggleTodoUseCase)
    factoryOf(::DeleteTodoUseCase)
}
```

- [ ] **Step 3: Create PresentationModule.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/di/PresentationModule.kt`:
```kotlin
package com.programovil.aura.di

import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TodoViewModel(get(), get(), get(), get()) }
}
```

- [ ] **Step 4: Create InitKoin.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`:
```kotlin
package com.programovil.aura.di

fun getModules() = listOf(
    dataModule,
    domainModule,
    presentationModule
)
```

- [ ] **Step 5: Delete old AppModules.kt**

Delete `composeApp/src/commonMain/kotlin/com/programovil/aura/di/AppModules.kt`

---

## Phase 2: Move Navigation to Root Level

### Task 2: Move Navigation Files

**Files:**
- Create: `navigation/NavRoute.kt`
- Create: `navigation/AppNavHost.kt`
- Delete: `presentation/navigation/NavRoute.kt`
- Delete: `presentation/navigation/AppNavHost.kt`

- [ ] **Step 1: Create NavRoute.kt in navigation/**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt`:
```kotlin
package com.programovil.aura.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Todo : NavRoute()
}
```

- [ ] **Step 2: Create AppNavHost.kt in navigation/**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`:
```kotlin
package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinInject()
    NavHost(navController = navController, startDestination = NavRoute.Todo) {
        composable<NavRoute.Todo> {
            TodoScreen(todoViewModel)
        }
    }
}
```

- [ ] **Step 3: Delete old navigation files**

Delete `composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/navigation/NavRoute.kt`
Delete `composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/navigation/AppNavHost.kt`

---

## Phase 3: Create Todo Use Cases

### Task 3: Create Domain Use Cases

**Files:**
- Create: `todo/domain/usecase/GetTodosUseCase.kt`
- Create: `todo/domain/usecase/AddTodoUseCase.kt`
- Create: `todo/domain/usecase/ToggleTodoUseCase.kt`
- Create: `todo/domain/usecase/DeleteTodoUseCase.kt`

- [ ] **Step 1: Create GetTodosUseCase.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/GetTodosUseCase.kt`:
```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow

class GetTodosUseCase(
    private val repository: TodoRepository
) {
    operator fun invoke(): Flow<Result<List<Todo>>> = repository.getTodos()
}
```

- [ ] **Step 2: Create AddTodoUseCase.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/AddTodoUseCase.kt`:
```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class AddTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(title: String): Result<Unit> = repository.addTodo(title)
}
```

- [ ] **Step 3: Create ToggleTodoUseCase.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/ToggleTodoUseCase.kt`:
```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class ToggleTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: String, isCompleted: Boolean): Result<Unit> =
        repository.toggleTodo(todoId, isCompleted)
}
```

- [ ] **Step 4: Create DeleteTodoUseCase.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/usecase/DeleteTodoUseCase.kt`:
```kotlin
package com.programovil.aura.todo.domain.usecase

import com.programovil.aura.todo.domain.repository.TodoRepository

class DeleteTodoUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: String): Result<Unit> = repository.deleteTodo(todoId)
}
```

---

## Phase 4: Create Todo Data Layer

### Task 4: Create TodoMapper

**Files:**
- Create: `todo/data/mapper/TodoMapper.kt`

- [ ] **Step 1: Create TodoMapper.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/mapper/TodoMapper.kt`:
```kotlin
package com.programovil.aura.todo.data.mapper

import com.programovil.aura.todo.domain.model.Todo

data class TodoData(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

fun TodoData.toDomain(): Todo = Todo(
    id = id,
    title = title,
    isCompleted = isCompleted
)
```

---

## Phase 5: Restructure Todo Presentation Layer

### Task 5: Create TodoViewModel with Use Cases

**Files:**
- Create: `todo/presentation/viewmodel/TodoViewModel.kt`

- [ ] **Step 1: Create TodoViewModel.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModel.kt`:
```kotlin
package com.programovil.aura.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoViewModel(
    private val getTodosUseCase: GetTodosUseCase,
    private val addTodoUseCase: AddTodoUseCase,
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

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            addTodoUseCase(title.trim())
                .onFailure { _error.value = "Failed to add todo" }
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleTodoUseCase(todoId, isCompleted)
                .onFailure { _error.value = "Failed to update todo" }
        }
    }

    fun deleteTodo(todoId: String) {
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

---

### Task 6: Create TodoItem Composable

**Files:**
- Create: `todo/presentation/composable/TodoItem.kt`

- [ ] **Step 1: Create TodoItem.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt`:
```kotlin
package com.programovil.aura.todo.presentation.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.todo.domain.model.Todo

@Composable
fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Text(
                    "X",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
            }
        }
    }
}
```

---

### Task 7: Create TodoScreen in screen/ folder

**Files:**
- Create: `todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Create TodoScreen.kt**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`:
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.sp
import com.programovil.aura.todo.presentation.composable.TodoItem
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var newTodoTitle by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Todos") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.addTodo(newTodoTitle)
                newTodoTitle = ""
            }) {
                Text("+", fontSize = 24.sp)
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
            OutlinedTextField(
                value = newTodoTitle,
                onValueChange = { newTodoTitle = it },
                label = { Text("New todo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                todos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No todos yet. Add your first one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                onDelete = { viewModel.deleteTodo(todo.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## Phase 6: Update TodoModule

### Task 8: Simplify TodoModule

**Files:**
- Modify: `todo/di/TodoModule.kt`

- [ ] **Step 1: Update TodoModule.kt**

Modify `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/di/TodoModule.kt`:
```kotlin
package com.programovil.aura.todo.di

import com.programovil.aura.todo.data.repository.TodoRepositoryImpl
import com.programovil.aura.todo.domain.repository.TodoRepository
import com.programovil.aura.todo.domain.usecase.AddTodoUseCase
import com.programovil.aura.todo.domain.usecase.DeleteTodoUseCase
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import com.programovil.aura.todo.domain.usecase.ToggleTodoUseCase
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val todoModule = module {
    // Data layer
    singleOf(::TodoRepositoryImpl).bind<TodoRepository>()

    // Domain layer - use cases
    factoryOf(::GetTodosUseCase)
    factoryOf(::AddTodoUseCase)
    factoryOf(::ToggleTodoUseCase)
    factoryOf(::DeleteTodoUseCase)

    // Presentation layer
    viewModel { TodoViewModel(get(), get(), get(), get()) }
}
```

---

## Phase 7: Update App.kt and AppNavHost Imports

### Task 9: Update App.kt

**Files:**
- Modify: `App.kt`

- [ ] **Step 1: Update App.kt imports and remove direct TodoScreen usage**

Modify `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`:
```kotlin
package com.programovil.aura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.navigation.AppNavHost
import org.koin.compose.koinInject

@Composable
@Preview
fun App(
    onSignInClick: () -> Unit = {}
) {
    MaterialTheme {
        val authViewModel: AuthViewModel = koinInject()
        val authState by authViewModel.authState.collectAsState()

        when (val state = authState) {
            is AuthViewModel.AuthState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthViewModel.AuthState.SignedIn -> {
                AppNavHost()
            }
            is AuthViewModel.AuthState.SignedOut,
            is AuthViewModel.AuthState.Error -> {
                SignInScreen(
                    errorMessage = if (state is AuthViewModel.AuthState.Error) state.message else null,
                    onSignInClick = onSignInClick
                )
            }
        }
    }
}

@Composable
fun SignInScreen(
    errorMessage: String?,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Aura",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sign in to sync your todos",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onSignInClick) {
            Text("Sign in with Google")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
```

---

## Phase 8: Clean Up Old Files

### Task 10: Delete obsolete TodoScreen and TodoViewModel from old location

**Files:**
- Delete: `todo/presentation/TodoScreen.kt` (old location)
- Delete: `todo/presentation/TodoViewModel.kt` (old location)

- [ ] **Step 1: Delete old TodoScreen.kt**

Delete `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/TodoScreen.kt`

- [ ] **Step 2: Delete old TodoViewModel.kt**

Delete `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/TodoViewModel.kt`

---

## Phase 9: Verify Build

### Task 11: Verify the project builds

- [ ] **Step 1: Run Gradle build**

Run: `./gradlew :composeApp:compileKotlinAndroid --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify no missing imports**

If build fails, check import statements in:
- `di/DataModule.kt` - verify `TodoRepositoryImpl` path
- `di/DomainModule.kt` - verify use case paths
- `di/PresentationModule.kt` - verify `TodoViewModel` path
- `navigation/AppNavHost.kt` - verify `TodoScreen` path

---

## Verification Checklist

After all tasks:
- [ ] `navigation/` folder exists at root level with `NavRoute.kt` and `AppNavHost.kt`
- [ ] `presentation/navigation/` folder is deleted
- [ ] `di/` has `DataModule.kt`, `DomainModule.kt`, `PresentationModule.kt`, `InitKoin.kt`
- [ ] `di/AppModules.kt` is deleted
- [ ] `todo/domain/usecase/` has 4 use case files
- [ ] `todo/data/mapper/TodoMapper.kt` exists
- [ ] `todo/presentation/screen/TodoScreen.kt` exists
- [ ] `todo/presentation/viewmodel/TodoViewModel.kt` exists
- [ ] `todo/presentation/composable/TodoItem.kt` exists
- [ ] Old `TodoScreen.kt` and `TodoViewModel.kt` at `todo/presentation/` root are deleted
- [ ] `App.kt` imports `AppNavHost` from `navigation/` package
- [ ] `./gradlew :composeApp:compileKotlinAndroid --no-daemon` succeeds
