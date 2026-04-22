# Firebase Auth + Firestore Todo — Phase 2 Spec

## Overview

Add Firebase Authentication (Google Sign-In) and Cloud Firestore (offline-first) to the existing KMP Clean Architecture todo app from Phase 1.

**Goal:** Replace hardcoded todos with cloud-synced, user-specific todos via Firestore, with Google Sign-In for user isolation.

---

## Architecture

### Clean Architecture Layers

```
domain/
├── model/
│   └── Todo.kt                    # Already exists (Phase 1)
└── repository/
    └── TodoRepository.kt          # Interface — no Firebase imports

data/
├── remote/
│   └── FirebaseConfig.kt         # Firebase + Firestore initialization
└── repository/
    └── TodoRepositoryImpl.kt      # Firestore implementation

presentation/
├── auth/
│   └── AuthViewModel.kt          # Google Sign-In state machine
└── todo/
    ├── TodoViewModel.kt          # Updated: uses TodoRepository
    └── TodoScreen.kt             # Updated: auth-aware, error snackbars
```

### Firestore Path Structure
```
users/{userId}/todos/{todoId}
```

Each user has their own subcollection under their Firebase Auth UID.

---

## Firebase Setup (User-Provided)

- **google-services.json:** already placed at `composeApp/src/androidMain/google-services.json`
- **Project ID:** `aura-app-7dce3`
- **Package:** `com.programovil.aura`

### Gradle Changes Required

**Root `build.gradle.kts`:**
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.4" apply false
}
```

**`composeApp/build.gradle.kts`:**
```kotlin
plugins {
    id("com.google.gms.google-services")  // applied, not deferred
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
}
```

---

## Components

### 1. `TodoRepository` (Domain Interface)

```kotlin
package org.example.aura_app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.aura_app.domain.model.Todo

interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String): Result<Unit>
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}
```

**Why `Flow<Result<T>>`:** Emits loading state, success, and failure through the same channel. Cleaner than separate loading/success/error states.

### 2. `FirebaseConfig` (Data)

```kotlin
package org.example.aura_app.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.initialize

object FirebaseConfig {
    fun initialize() {
        Firebase.initialize(context)
    }

    val auth = Firebase.auth
    val firestore = Firebase.firestore.apply {
        // Enable offline persistence
        firebaseFirestoreSettings = firebaseFirestoreSettings {
            isPersistenceEnabled = true
        }
    }
}
```

### 3. `TodoRepositoryImpl` (Data Implementation)

```kotlin
package org.example.aura_app.data.repository

import com.google.firebase.firestore.collectForFlux
import com.google.firebase.firestore.firebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.example.aura_app.domain.model.Todo
import org.example.aura_app.domain.repository.TodoRepository
import org.example.aura_app.data.remote.FirebaseConfig

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
            "createdAt" to System.currentTimeMillis()
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

### 4. `AuthViewModel` (Presentation — Auth)

```kotlin
package org.example.aura_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.aura_app.data.remote.FirebaseConfig

class AuthViewModel : ViewModel() {

    sealed class AuthState {
        data object Loading : AuthState()
        data object SignedIn : AuthState()
        data object SignedOut : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    val currentUser: FirebaseUser?
        get() = FirebaseConfig.auth.currentUser

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        _authState.value = if (FirebaseConfig.auth.currentUser != null) {
            AuthState.SignedIn
        } else {
            AuthState.SignedOut
        }
    }

    fun signIn(result: ActivityResult) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // Google Sign-In result handling
            // On success: _authState.value = AuthState.SignedIn
            // On failure: _authState.value = AuthState.SignedOut
        }
    }
}
```

### 5. Updated `TodoViewModel` (Presentation)

Replace hardcoded `MutableStateFlow` with repository calls:

```kotlin
class TodoViewModel(
    private val repository: TodoRepository = TodoRepositoryImpl()
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
                    .onFailure { _error.value = it.message }
                _isLoading.value = false
            }
        }
    }

    fun addTodo(title: String) {
        viewModelScope.launch {
            repository.addTodo(title)
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

    fun clearError() { _error.value = null }
}
```

### 6. Updated `TodoScreen` (Presentation)

- Check auth state: if signed out, show splash/redirect
- Show `CircularProgressIndicator` while loading
- Display todos in a list with checkbox + delete button
- Show snackbar on error (`LaunchedEffect` on `_error`)
- Add a text field + button for adding new todos

### 7. Updated `AppModules.kt` (DI)

Add repository to KOIN:

```kotlin
val dataModule = module {
    single { TodoRepositoryImpl() }
}
```

### 8. Splash Screen (Entry Point)

```kotlin
@Composable
fun SplashScreen(onNavigateToTodos: () -> Unit) {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.SignedIn -> onNavigateToTodos()
            is AuthViewModel.AuthState.SignedOut -> { /* trigger Google Sign-In */ }
            AuthViewModel.AuthState.Loading -> { /* stay on splash */ }
        }
    }

    Box(fillMaxSize(), contentAlignment = Center) {
        CircularProgressIndicator()
    }
}
```

---

## Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/todos/{todoId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Each user can only read/write their own todos.

---

## KOIN Dependency Injection

```kotlin
// di/AppModules.kt — update dataModule
val dataModule = module {
    single { TodoRepositoryImpl() }
}
```

---

## Error Handling

- **Network/Firestore errors:** Snackbar "Connection issue, retrying..." with auto-retry via Firestore's offline persistence
- **Auth errors:** Redirect to Google Sign-In
- **Empty state:** "No todos yet. Add your first one!" message

---

## Summary of Changes by File

| File | Action |
|------|--------|
| `build.gradle.kts` (root) | Add google-services plugin |
| `composeApp/build.gradle.kts` | Add google-services plugin + Firebase SDKs |
| `gradle/libs.versions.toml` | Add Firebase library entries |
| `domain/repository/TodoRepository.kt` | **Create** — interface |
| `data/remote/FirebaseConfig.kt` | **Create** — Firebase init |
| `data/repository/TodoRepositoryImpl.kt` | **Create** — Firestore implementation |
| `presentation/auth/AuthViewModel.kt` | **Create** — auth state machine |
| `presentation/todo/TodoViewModel.kt` | **Modify** — use repository |
| `presentation/todo/TodoScreen.kt` | **Modify** — add todos UI, error handling |
| `di/AppModules.kt` | **Modify** — add repository to dataModule |
| `App.kt` | **Modify** — add splash/auth flow |

---

## Out of Scope (Phase 3)

- Sentry integration
- Room database
- iOS CocoaPods setup
- GitFlow branch structure
- Todo editing (update title)
