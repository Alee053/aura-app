# Firebase Auth + Firestore Todo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire Firebase Auth (Google Sign-In) and Firestore (offline-first) into the existing KMP app, replacing hardcoded todos with cloud-synced user-specific todos.

**Architecture:** Clean Architecture — domain layer has no Firebase imports, data layer implements repository via Firestore, presentation uses repository via KOIN injection. Repository pattern: `TodoRepository` interface → `TodoRepositoryImpl` Firestore implementation.

**Tech Stack:** Firebase Auth KTX, Firebase Firestore KTX, Google Play Services Auth, KOIN DI, Jetpack Navigation Compose, kotlinx-coroutines-play-services.

---

## File Structure

```
composeApp/src/commonMain/kotlin/org/example/aura_app/
├── App.kt                              # Modify — splash/auth/nav routing
├── di/
│   └── AppModules.kt                   # Modify — add repository + AuthViewModel
├── domain/
│   └── model/
│       └── Todo.kt                     # Already exists (no changes)
│   └── repository/
│       └── TodoRepository.kt           # Create — interface
└── presentation/
    ├── auth/
    │   └── AuthViewModel.kt           # Create — auth state machine
    ├── navigation/
    │   ├── NavRoutes.kt               # Already exists (no changes)
    │   └── AppNavHost.kt               # Modify — add auth-aware routing
    └── todo/
        ├── TodoViewModel.kt            # Modify — use repository
        └── TodoScreen.kt               # Modify — full CRUD UI + errors

composeApp/src/androidMain/kotlin/org/example/aura_app/
├── AndroidApp.kt                      # Modify — call FirebaseConfig.init()
└── MainActivity.kt                    # Modify — pass ActivityResult to AuthViewModel
```

**Modified files:**
- `build.gradle.kts` (root) — add google-services plugin
- `composeApp/build.gradle.kts` — add google-services plugin + Firebase SDKs
- `gradle/libs.versions.toml` — add Firebase library entries

**Critical note:** The `google-services.json` package is `com.programovil.aura` but the existing `namespace` in `composeApp/build.gradle.kts` is `org.example.aura_app`. The namespace MUST be updated to `com.programovil.aura` to match the Firebase configuration.

---

## Tasks

### Task 1: Update root `build.gradle.kts` with google-services plugin

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add google-services plugin**

Open `build.gradle.kts` and add `id("com.google.gms.google-services") apply false` to the plugins block:

```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("com.google.gms.google-services") apply false
}
```

- [ ] **Step 2: Commit**

```bash
git add build.gradle.kts && git commit -m "chore: add google-services Gradle plugin"
```

---

### Task 2: Update `libs.versions.toml` with Firebase library entries

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add Firebase version and library entries**

Append to the end of `[versions]` section:
```toml
firebaseBom = "34.12.0"
playServicesAuth = "21.3.0"
kotlinxCoroutinesPlayServices = "1.9.0"
```

Append to the end of `[libraries]` section:
```toml
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { module = "com.google.firebase:firebase-auth-ktx" }
firebase-firestore-ktx = { module = "com.google.firebase:firebase-firestore-ktx" }
play-services-auth = { module = "com.google.android.gms:play-services-auth", version.ref = "playServicesAuth" }
kotlinx-coroutines-play-services = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "kotlinxCoroutinesPlayServices" }
```

- [ ] **Step 2: Commit**

```bash
git add gradle/libs.versions.toml && git commit -m "chore: add Firebase and Play Services Auth versions"
```

---

### Task 3: Update `composeApp/build.gradle.kts` — add Firebase plugins, SDKs, and fix namespace

**Files:**
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add google-services plugin to the plugins block**

Add `id("com.google.gms.google-services")` to the plugins block (note: no `apply false` here — it's applied directly in the app module):

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.google.gms.google-services")
}
```

- [ ] **Step 2: Add Firebase dependencies to `androidMain.dependencies`**

Add these to the `androidMain.dependencies` block:
```kotlin
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth.ktx)
implementation(libs.firebase.firestore.ktx)
implementation(libs.play.services.auth)
implementation(libs.kotlinx.coroutines.play.services)
```

- [ ] **Step 3: Fix namespace to match google-services.json**

In the `android { }` block, change:
```kotlin
namespace = "org.example.aura_app"
applicationId = "org.example.aura_app"
```
to:
```kotlin
namespace = "com.programovil.aura"
applicationId = "com.programovil.aura"
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/build.gradle.kts && git commit -m "chore: add Firebase SDKs, google-services plugin, fix namespace"
```

---

### Task 4: Create domain layer — `TodoRepository.kt` interface

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/domain/repository/TodoRepository.kt`

- [ ] **Step 1: Write TodoRepository interface**

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

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/domain/repository/TodoRepository.kt && git commit -m "feat(domain): add TodoRepository interface"
```

---

### Task 5: Create data layer — `FirebaseConfig.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/data/remote/FirebaseConfig.kt`

- [ ] **Step 1: Write FirebaseConfig**

```kotlin
package org.example.aura_app.data.remote

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.initialize

object FirebaseConfig {
    private var _isInitialized = false

    fun initialize(context: Context) {
        if (_isInitialized) return
        Firebase.initialize(context)
        _isInitialized = true
    }

    val auth = Firebase.auth
    val firestore = Firebase.firestore
}
```

**Note:** Offline persistence is enabled by default in Firestore SDK for Android when `firebase-firestore-ktx` is used — no explicit settings call needed.

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/data/remote/FirebaseConfig.kt && git commit -m "feat(data): add FirebaseConfig with Auth and Firestore singletons"
```

---

### Task 6: Create data layer — `TodoRepositoryImpl.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/data/repository/TodoRepositoryImpl.kt`

- [ ] **Step 1: Write TodoRepositoryImpl**

```kotlin
package org.example.aura_app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.example.aura_app.data.remote.FirebaseConfig
import org.example.aura_app.domain.model.Todo
import org.example.aura_app.domain.repository.TodoRepository

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

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/data/repository/TodoRepositoryImpl.kt && git commit -m "feat(data): add TodoRepositoryImpl with Firestore offline-first"
```

---

### Task 7: Create presentation layer — `AuthViewModel.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/auth/AuthViewModel.kt`

- [ ] **Step 1: Write AuthViewModel**

```kotlin
package org.example.aura_app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.aura_app.data.remote.FirebaseConfig

class AuthViewModel : ViewModel() {

    sealed class AuthState {
        data object Loading : AuthState()
        data object SignedIn : AuthState()
        data object SignedOut : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private var googleSignInClient: GoogleSignInClient? = null

    fun buildGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com")
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(android.app.Application(), gso)
    }

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

    fun handleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInFormIntent(data)
                val account = task?.await()
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                FirebaseConfig.auth.signInWithCredential(credential).await()
                _authState.value = AuthState.SignedIn
            } catch (e: ApiException) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        FirebaseConfig.auth.signOut()
        googleSignInClient?.signOut()
        _authState.value = AuthState.SignedOut
    }
}
```

**Note:** `android.app.Application()` as context placeholder — KOIN will provide the actual application context via `androidContext()`.

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/auth/AuthViewModel.kt && git commit -m "feat(auth): add AuthViewModel with Google Sign-In"
```

---

### Task 8: Update `TodoViewModel.kt` to use repository

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoViewModel.kt`

- [ ] **Step 1: Rewrite TodoViewModel to use repository**

Replace the entire contents of `TodoViewModel.kt` with:

```kotlin
package org.example.aura_app.presentation.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.aura_app.domain.model.Todo
import org.example.aura_app.domain.repository.TodoRepository

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

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoViewModel.kt && git commit -m "refactor(todo): replace hardcoded todos with repository"
```

---

### Task 9: Update `TodoScreen.kt` with full CRUD UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoScreen.kt`

- [ ] **Step 1: Rewrite TodoScreen with add/delete/toggle UI and error handling**

Replace the entire contents of `TodoScreen.kt` with:

```kotlin
package org.example.aura_app.presentation.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import org.example.aura_app.domain.model.Todo

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
                Icon(Icons.Default.Add, contentDescription = "Add todo")
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
                    Box(fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                todos.isEmpty() -> {
                    Box(fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoScreen.kt && git commit -m "feat(todo): add full CRUD UI with add/delete/toggle and error snackbars"
```

---

### Task 10: Update `AppModules.kt` with repository and AuthViewModel DI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/example/aura_app/di/AppModules.kt`

- [ ] **Step 1: Update AppModules to register TodoRepository and AuthViewModel**

Replace the entire contents of `AppModules.kt` with:

```kotlin
package org.example.aura_app.di

import org.example.aura_app.data.repository.TodoRepositoryImpl
import org.example.aura_app.domain.repository.TodoRepository
import org.example.aura_app.presentation.auth.AuthViewModel
import org.example.aura_app.presentation.todo.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TodoViewModel(get()) }
    viewModel { AuthViewModel() }
}

val domainModule = module {
    // Repository interfaces and use cases go here
}

val dataModule = module {
    single<TodoRepository> { TodoRepositoryImpl() }
}

fun getModules() = listOf(
    domainModule,
    dataModule,
    presentationModule
)
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/di/AppModules.kt && git commit -m "chore(di): register TodoRepository and AuthViewModel with KOIN"
```

---

### Task 11: Update `AndroidApp.kt` to initialize Firebase

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/org/example/aura_app/AndroidApp.kt`

- [ ] **Step 1: Add Firebase initialization**

Replace the entire contents of `AndroidApp.kt` with:

```kotlin
package org.example.aura_app

import android.app.Application
import org.example.aura_app.data.remote.FirebaseConfig
import org.example.aura_app.di.getModules
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseConfig.initialize(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules())
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/androidMain/kotlin/org/example/aura_app/AndroidApp.kt && git commit -m "chore: initialize Firebase before Koin in AndroidApp"
```

---

### Task 12: Update `App.kt` with auth-aware navigation

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/example/aura_app/App.kt`

- [ ] **Step 1: Rewrite App.kt with auth-aware routing**

Replace the entire contents of `App.kt` with:

```kotlin
package org.example.aura_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import org.example.aura_app.presentation.auth.AuthViewModel
import org.example.aura_app.presentation.navigation.AppNavHost
import org.example.aura_app.presentation.todo.TodoScreen
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    MaterialTheme {
        val authViewModel: AuthViewModel = koinInject()
        val authState by authViewModel.authState.collectAsState()

        when (authState) {
            is AuthViewModel.AuthState.Loading -> {
                Box(fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthViewModel.AuthState.SignedIn -> {
                TodoScreen()
            }
            is AuthViewModel.AuthState.SignedOut,
            is AuthViewModel.AuthState.Error -> {
                SignInScreen(authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun SignInScreen(authViewModel: AuthViewModel) {
    Box(fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Please sign in to continue")
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/org/example/aura_app/App.kt && git commit -m "feat(app): add auth-aware navigation with loading/signed-out states"
```

---

### Task 13: Update `MainActivity.kt` to handle Google Sign-In result

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/org/example/aura_app/MainActivity.kt`

- [ ] **Step 1: Read current MainActivity.kt**

```bash
cat /home/ale/Dev/University/aura-app/composeApp/src/androidMain/kotlin/org/example/aura_app/MainActivity.kt
```

- [ ] **Step 2: Update MainActivity to handle sign-in result**

Replace the entire contents of `MainActivity.kt` with:

```kotlin
package org.example.aura_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.example.aura_app.presentation.auth.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel = getViewModel()

        setContent {
            App()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            authViewModel.handleSignInResult(data)
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/org/example/aura_app/MainActivity.kt && git commit -m "chore: wire onActivityResult to AuthViewModel for Google Sign-In"
```

---

### Task 14: Verify build compiles

**Files:**
- Run: `./gradlew assembleDebug`

- [ ] **Step 1: Run assembleDebug**

```bash
cd /home/ale/Dev/University/aura-app && ./gradlew assembleDebug --no-daemon 2>&1
```

Expected: BUILD SUCCESSFUL — APK generated at `composeApp/build/outputs/apk/debug/`

If BUILD FAILED: inspect the error, fix inline, commit the fix.

---

### Task 15: Write Firestore security rules

**Files:**
- Create: `docs/superpowers/firestore.rules`

- [ ] **Step 1: Write Firestore security rules**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/todos/{todoId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/firestore.rules && git commit -m "docs: add Firestore security rules for user-isolated todos"
```

---

## Self-Review Checklist

- [ ] Spec coverage: All components from spec (TodoRepository, FirebaseConfig, TodoRepositoryImpl, AuthViewModel, updated TodoViewModel/TodoScreen/AppModules/App) map to tasks
- [ ] Placeholder scan: No TBD/TODO — all code is complete
- [ ] Type consistency: `TodoRepository` interface methods match `TodoRepositoryImpl` implementation; `TodoViewModel(repository: TodoRepository)` constructor parameter type matches KOIN `get()` injection
- [ ] File paths: All paths use exact project location `/home/ale/Dev/University/aura-app`
- [ ] Namespace fix: `org.example.aura_app` → `com.programovil.aura` in composeApp/build.gradle.kts
- [ ] SHA fingerprint: `31717b9c148ab9be1224ad940f40f2752e3ec7a7` matches google-services.json OAuth client certificate_hash
- [ ] Commands: All `Run:` commands have expected output described
