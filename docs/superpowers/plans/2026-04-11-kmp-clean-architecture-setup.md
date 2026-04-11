# KMP Clean Architecture Setup Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire up KOIN DI, navigation, and Clean Architecture folder structure with a hardcoded todo screen that compiles and launches.

**Architecture:** Clean Architecture with three layers — Domain (models, repository interfaces), Data (repository implementations, local data sources), Presentation (ViewModels, Screens). KOIN for dependency injection across all layers. Jetpack Navigation Compose with type-safe routes for screen navigation.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, KOIN 4.1.1, Navigation Compose 2.9.2, kotlinx-serialization-json 1.8.0, Room (stubbed), Firebase (deferred to TODO).

---

## File Structure

```
composeApp/src/commonMain/kotlin/org/example/aura_app/
├── App.kt                          # Root composable — wire NavHost here
├── di/
│   └── AppModules.kt               # KOIN module definitions (domain, data, presentation)
├── domain/
│   └── model/
│       └── Todo.kt                 # Domain entity
├── presentation/
│   ├── navigation/
│   │   ├── NavRoutes.kt            # Sealed class NavRoute with type-safe routes
│   │   └── AppNavHost.kt           # NavHost with composable<NavRoute.Todo>
│   └── todo/
│       ├── TodoViewModel.kt        # ViewModel with dummy state, extends ViewModel
│       └── TodoScreen.kt           # Composable showing hardcoded todo list
└── AndroidApp.kt                  # Application class calling startKoin

composeApp/src/androidMain/kotlin/org/example/aura_app/
├── AndroidApp.kt                  # Application class calling startKoin
└── MainActivity.kt                # Already exists — keep, no changes needed
```

**Modified files:**
- `composeApp/build.gradle.kts` — add KOIN and navigation plugins + dependencies
- `gradle/libs.versions.toml` — add KOIN and navigation versions + libraries

**Deleted files:**
- `composeApp/src/commonMain/kotlin/org/example/aura_app/Greeting.kt` — replaced by TodoScreen
- `composeApp/src/commonMain/kotlin/org/example/aura_app/Platform.kt` — not needed for setup

---

## Tasks

### Task 1: Add dependencies to `libs.versions.toml`

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add version entries and libraries**

Append at the end of `[versions]` section (before the closing bracket):
```toml
koin = "4.1.1"
navigationCompose = "2.9.2"
kotlinxSerializationJson = "1.8.0"
```

Append at the end of `[libraries]` section:
```toml
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
```

Append at the end of `[plugins]` section:
```toml
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Run: `cd /home/ale/Dev/University/aura-app && ./gradlew --stop` to terminate any running daemons.

---

### Task 2: Update `composeApp/build.gradle.kts` with KOIN and Navigation plugins + dependencies

**Files:**
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add plugins to the `plugins` block**

Add `alias(libs.plugins.kotlinSerialization)` to the `plugins` block.

- [ ] **Step 2: Add KOIN and Navigation dependencies to `androidMain.dependencies`**

Add these implementations to the `androidMain.dependencies` block:
```kotlin
implementation(libs.koin.compose)
implementation(libs.navigation.compose)
```

- [ ] **Step 3: Add KOIN, Navigation, and Serialization dependencies to `commonMain.dependencies`**

Add these implementations to the `commonMain.dependencies` block:
```kotlin
implementation(libs.koin.core)
implementation(libs.koin.compose)
implementation(libs.koin.compose.viewmodel)
implementation(libs.navigation.compose)
implementation(libs.kotlinx.serialization.json)
```

Run: `cd /home/ale/Dev/University/aura-app && ./gradlew :composeApp:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -E "(koin|navigation|serialization)" | head -20` to verify the new dependencies resolve. Expected output shows `io.insert-koin:koin-core` and `org.jetbrains.androidx.navigation:navigation-compose` without resolution errors.

---

### Task 3: Create domain layer — `Todo.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/domain/model/Todo.kt`

- [ ] **Step 1: Write Todo model**

```kotlin
package org.example.aura_app.domain.model

data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)
```

---

### Task 4: Create presentation navigation — `NavRoutes.kt` and `AppNavHost.kt`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/navigation/NavRoutes.kt`
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/navigation/AppNavHost.kt`

- [ ] **Step 1: Write NavRoutes.kt**

```kotlin
package org.example.aura_app.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Todo : NavRoute()
}
```

- [ ] **Step 2: Write AppNavHost.kt**

```kotlin
package org.example.aura_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.example.aura_app.presentation.todo.TodoScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = NavRoute.Todo) {
        composable<NavRoute.Todo> {
            TodoScreen()
        }
    }
}
```

---

### Task 5: Create TodoViewModel and TodoScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoScreen.kt`

- [ ] **Step 1: Write TodoViewModel.kt**

```kotlin
package org.example.aura_app.presentation.todo

import androidx.lifecycle.ViewModel
import org.example.aura_app.domain.model.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodoViewModel : ViewModel() {
    private val _todos = MutableStateFlow(
        listOf(
            Todo(id = "1", title = "Buy groceries", isCompleted = false),
            Todo(id = "2", title = "Read a book", isCompleted = true),
            Todo(id = "3", title = "Exercise", isCompleted = false)
        )
    )
    val todos: StateFlow<List<Todo>> = _todos.asStateFlow()
}
```

- [ ] **Step 2: Write TodoScreen.kt**

```kotlin
package org.example.aura_app.presentation.todo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.aura_app.domain.model.Todo
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodoScreen(viewModel: TodoViewModel = koinViewModel<TodoViewModel>()) {
    val todos by viewModel.todos.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Todo List", style = MaterialTheme.typography.headlineMedium)
        todos.forEach { todo ->
            Text(
                text = if (todo.isCompleted) "✓ ${todo.title}" else "○ ${todo.title}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
```

---

### Task 6: Create KOIN DI modules

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/example/aura_app/di/AppModules.kt`

- [ ] **Step 1: Write AppModules.kt**

```kotlin
package org.example.aura_app.di

import org.example.aura_app.presentation.todo.TodoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { TodoViewModel() }
}

val domainModule = module {
    // Repository interfaces and use cases go here
}

val dataModule = module {
    // Repository implementations and data sources go here
}

fun getModules() = listOf(
    domainModule,
    dataModule,
    presentationModule
)
```

---

### Task 7: Create AndroidApp Application class

**Files:**
- Create: `composeApp/src/androidMain/kotlin/org/example/aura_app/AndroidApp.kt`

- [ ] **Step 1: Write AndroidApp.kt**

```kotlin
package org.example.aura_app

import android.app.Application
import org.example.aura_app.di.getModules
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules())
        }
    }
}
```

---

### Task 8: Update AndroidManifest to use AndroidApp

**Files:**
- Modify: `composeApp/src/androidMain/AndroidManifest.xml` (check if exists or create)

- [ ] **Step 1: Add `android:name=".AndroidApp"` to the `<application>` tag**

Open `composeApp/src/androidMain/AndroidManifest.xml` and add `android:name=".AndroidApp"` to the `<application>` opening tag, so it reads:
```xml
<application
    android:name=".AndroidApp"
    android:allowBackup="true"
    ...>

---

### Task 9: Rewrite App.kt to use AppNavHost

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/example/aura_app/App.kt`

- [ ] **Step 1: Rewrite App.kt**

```kotlin
package org.example.aura_app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.aura_app.presentation.navigation.AppNavHost

@Composable
@Preview
fun App() {
    MaterialTheme {
        AppNavHost()
    }
}
```

---

### Task 10: Clean up old files

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/org/example/aura_app/Greeting.kt`
- Delete: `composeApp/src/commonMain/kotlin/org/example/aura_app/Platform.kt`

Run: `rm /home/ale/Dev/University/aura-app/composeApp/src/commonMain/kotlin/org/example/aura_app/Greeting.kt /home/ale/Dev/University/aura-app/composeApp/src/commonMain/kotlin/org/example/aura_app/Platform.kt`

---

### Task 11: Verify the build compiles and launches

**Files:**
- Run: `./gradlew assembleDebug`

- [ ] **Step 1: Run assembleDebug**

Run: `cd /home/ale/Dev/University/aura-app && ./gradlew assembleDebug --no-daemon 2>&1`
Expected: BUILD SUCCESSFUL — APK generated at `composeApp/build/outputs/apk/debug/`

- [ ] **Step 2: Report result**

If BUILD SUCCESSFUL, report the APK path.
If BUILD FAILED, inspect the error and fix inline.

---

## External Setup Deferred (TODO for User)

The following are out of scope for this plan — document in a TODO file:
- Firebase project setup (Realtime Database, Remote Config)
- Room database setup and entity definitions
- Sentry integration
- iOS CocoaPods setup
- GitFlow branch structure

---

## Self-Review Checklist

- [ ] Spec coverage: All 11 tasks map to the design (DI wiring, navigation, clean architecture folders, hardcoded todo screen)
- [ ] Placeholder scan: No TBD/TODO/implement later in task steps — all have actual code
- [ ] Type consistency: `TodoViewModel` uses `koinViewModel<TodoViewModel>()`, `NavRoute.Todo` matches `composable<NavRoute.Todo>`, `Todo(id, title, isCompleted)` matches usage in `TodoScreen`
- [ ] File paths: All paths use the exact project location `/home/ale/Dev/University/aura-app`
- [ ] Commands: All `Run:` commands have expected output described