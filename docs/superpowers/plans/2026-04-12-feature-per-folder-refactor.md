# Feature-Per-Folder Clean Architecture Refactor

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor from layer-based to feature-based architecture, moving `data/`, `domain/`, `presentation/` from being top-level folders to being inside each feature folder.

**Architecture:** Each feature (auth, todo) becomes a self-contained module at root level containing its own clean architecture layers (data/, domain/, presentation/, di/). Cross-cutting infrastructure (FirebaseConfig) lives in a shared/ folder at root. Root-level di/AppModules.kt coordinates all feature DI modules.

**Tech Stack:** Kotlin Multiplatform, Koin DI, Firebase Firestore, Clean Architecture

---

## File Structure

```
src/commonMain/kotlin/com/programovil/aura/
├── auth/
│   └── presentation/AuthViewModel.kt
│   └── di/AuthModule.kt
│   └── (domain/, data/ created empty for future use)
├── todo/
│   ├── data/repository/TodoRepositoryImpl.kt
│   ├── domain/model/Todo.kt
│   ├── domain/repository/TodoRepository.kt
│   ├── presentation/TodoScreen.kt
│   ├── presentation/TodoViewModel.kt
│   └── di/TodoModule.kt
├── shared/FirebaseConfig.kt
├── di/AppModules.kt
└── App.kt
```

---

## Task 1: Create auth/ Feature Structure

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/auth/di/AuthModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/auth/AuthViewModel.kt` → move to `auth/presentation/`

- [ ] **Step 1: Create auth/ directory structure**

```bash
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/auth/{domain,data,presentation,di}
```

- [ ] **Step 2: Create auth/di/AuthModule.kt**

```kotlin
package com.programovil.aura.auth.di

import com.programovil.aura.auth.presentation.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    viewModel { AuthViewModel() }
}
```

- [ ] **Step 3: Move AuthViewModel to auth/presentation/ and update package**

File: `composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/auth/AuthViewModel.kt`
Move to: `composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/AuthViewModel.kt`

New package declaration: `package com.programovil.aura.auth.presentation`
New import for FirebaseConfig: `import com.programovil.aura.shared.FirebaseConfig`

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/auth/
git commit -m "feat: create auth feature with di module"
```

---

## Task 2: Create shared/FirebaseConfig.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/data/remote/FirebaseConfig.kt` → move to `shared/FirebaseConfig.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/AuthViewModel.kt` (update import)
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt` (update import - from Task 3)

- [ ] **Step 1: Create shared/ directory**

```bash
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/shared
```

- [ ] **Step 2: Create shared/FirebaseConfig.kt with updated package**

```kotlin
package com.programovil.aura.shared

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

object FirebaseConfig {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
}
```

- [ ] **Step 3: Delete old FirebaseConfig.kt**

```bash
rm composeApp/src/commonMain/kotlin/com/programovil/aura/data/remote/FirebaseConfig.kt
rmdir composeApp/src/commonMain/kotlin/com/programovil/aura/data/remote 2>/dev/null || true
rmdir composeApp/src/commonMain/kotlin/com/programovil/aura/data 2>/dev/null || true
```

- [ ] **Step 4: Commit**

```bash
git add shared/
git rm composeApp/src/commonMain/kotlin/com/programovil/aura/data/remote/FirebaseConfig.kt
git commit -m "feat: move FirebaseConfig to shared/"
```

---

## Task 3: Create todo/ Feature Structure

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/model/Todo.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/domain/repository/TodoRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/TodoViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/TodoScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/di/TodoModule.kt`

- [ ] **Step 1: Create todo/ directory structure**

```bash
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/todo/{domain/model,domain/repository,data/repository,presentation,di}
```

- [ ] **Step 2: Create todo/domain/model/Todo.kt**

```kotlin
package com.programovil.aura.todo.domain.model

data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)
```

- [ ] **Step 3: Create todo/domain/repository/TodoRepository.kt**

```kotlin
package com.programovil.aura.todo.domain.repository

import com.programovil.aura.todo.domain.model.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String): Result<Unit>
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}
```

- [ ] **Step 4: Create todo/data/repository/TodoRepositoryImpl.kt**

```kotlin
package com.programovil.aura.todo.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TodoRepositoryImpl : TodoRepository {

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
                    Todo(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        isCompleted = doc.getBoolean("isCompleted") ?: false
                    )
                } ?: emptyList()
                trySend(Result.success(todos))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addTodo(title: String): Result<Unit> = runCatching {
        userTodosCollection().add(mapOf(
            "title" to title,
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
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

- [ ] **Step 5: Create todo/presentation/TodoViewModel.kt**

```kotlin
package com.programovil.aura.todo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: TodoRepository
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
            repository.getTodos().collect { result ->
                result.onSuccess { _todos.value = it }
                    .onFailure { _error.value = it.message ?: "Unknown error" }
                _isLoading.value = false
            }
        }
    }

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTodo(title.trim())
                .onFailure { _error.value = "Failed to add todo" }
        }
    }

    fun toggleTodo(todoId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTodo(todoId, isCompleted)
                .onFailure { _error.value = "Failed to update todo" }
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            repository.deleteTodo(todoId)
                .onFailure { _error.value = "Failed to delete todo" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
```

- [ ] **Step 6: Create todo/presentation/TodoScreen.kt**

```kotlin
package com.programovil.aura.todo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programovil.aura.todo.domain.model.Todo

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

@Composable
private fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

- [ ] **Step 7: Create todo/di/TodoModule.kt**

```kotlin
package com.programovil.aura.todo.di

import com.programovil.aura.todo.data.repository.TodoRepositoryImpl
import com.programovil.aura.todo.domain.repository.TodoRepository
import org.koin.dsl.module

val todoModule = module {
    single<TodoRepository> { TodoRepositoryImpl() }
}
```

- [ ] **Step 8: Delete old todo files**

```bash
rm -rf composeApp/src/commonMain/kotlin/com/programovil/aura/domain
rm -rf composeApp/src/commonMain/kotlin/com/programovil/aura/data
rm -rf composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/todo
rm -rf composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/auth
```

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/
git rm -r composeApp/src/commonMain/kotlin/com/programovil/aura/domain/
git rm -r composeApp/src/commonMain/kotlin/com/programovil/aura/data/
git commit -m "feat: create todo feature with clean architecture layers"
```

---

## Task 4: Update root-level di/AppModules.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/AppModules.kt`

- [ ] **Step 1: Rewrite AppModules.kt with feature module imports**

```kotlin
package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.todo.di.todoModule
import com.programovil.aura.todo.presentation.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { AuthViewModel() }
    viewModel { TodoViewModel(get()) }
}

val domainModule = module {
    // Repository interfaces and use cases go here
}

val dataModule = module {
    // Data layer specifics if needed
}

fun getModules() = listOf(
    authModule,
    todoModule,
    domainModule,
    dataModule,
    presentationModule
)
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/di/AppModules.kt
git commit -m "refactor: update AppModules to import feature DI modules"
```

---

## Task 5: Update App.kt imports

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Update App.kt imports to use new package paths**

```kotlin
package com.programovil.aura

// ... existing imports ...

import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.todo.presentation.TodoScreen
import com.programovil.aura.todo.presentation.TodoViewModel
import org.koin.compose.koinInject
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "refactor: update App.kt imports for new package structure"
```

---

## Task 6: Verify Build

- [ ] **Step 1: Run assemble to verify compilation**

```bash
./gradlew assemble
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit verification**

```bash
git add -A
git commit -m "chore: verify build after refactor"
```

---

## Verification

1. `./gradlew assemble` succeeds
2. App launches and auth flow works
3. Todo list displays after sign-in
4. DI resolves all dependencies without errors