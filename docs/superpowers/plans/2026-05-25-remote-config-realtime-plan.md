# Real-Time Feature Flag Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Firebase Remote Config real-time listener support so feature flags (Todos, Habits) hot-reload without app restart, and guard all UI entry points to disabled features.

**Architecture:** Firebase `addOnConfigUpdateListener` (Android only) pushes config changes to `RemoteConfigService`, which notifies `FeatureFlagManager` via callback. `FeatureFlagManager` updates its `StateFlow`, and Compose UI reactively hides bottom nav tabs, NavHost routes, and Home dashboard cards. iOS stub remains unchanged (defaults only, no-op listener).

**Tech Stack:** Kotlin Multiplatform, Firebase Remote Config, Jetpack Compose Navigation, Koin DI, Mockative + Turbine (testing)

---

## File Structure

| File | Action | Responsibility |
|------|--------|--------------|
| `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt` | Modify | Add `registerOnConfigUpdateListener()` to interface |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt` | Modify | Register listener in `initialize()`, refresh flags on callback |
| `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt` | Modify | Implement real-time listener with `addOnConfigUpdateListener` |
| `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt` | Modify | Add no-op `registerOnConfigUpdateListener` |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt` | Modify | Accept `featureFlags`, conditionally register routes, guard click handlers |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt` | Modify | Accept `showTodos`/`showHabits` booleans, conditionally render cards |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt` | Modify | Accept `featureFlags` and `onFeatureDisabled`, pop back when disabled |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt` | Modify | Accept `featureFlags` and `onFeatureDisabled`, pop back when disabled |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt` | Modify | Pass `featureFlags` down to `AppNavHost` and screens |
| `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt` | Create | Unit tests for listener-driven flag updates |

---

## Task 1: Update `RemoteConfigService` Interface

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt`

- [ ] **Step 1: Add `@Mockable` annotation and `registerOnConfigUpdateListener`**

```kotlin
package com.programovil.aura.shared

import io.mockative.Mockable

@Mockable
interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun getString(flag: FeatureFlag, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>

    /** Register a callback that is invoked when remote config values change. */
    fun registerOnConfigUpdateListener(onUpdate: () -> Unit)
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt
git commit -m "feat(remote-config): add config update listener to RemoteConfigService interface"
```

---

## Task 2: Update `FeatureFlagManager`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt`

- [ ] **Step 1: Refactor `initialize()` to register listener and extract `refreshFlags()`**

```kotlin
package com.programovil.aura.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        // Initial fetch and activation
        remoteConfigService.fetchAndActivate()
        refreshFlags()

        // Register real-time listener for subsequent updates
        remoteConfigService.registerOnConfigUpdateListener {
            refreshFlags()
        }
    }

    private fun refreshFlags() {
        _flags.value = FeatureFlag.entries.associateWith { flag ->
            remoteConfigService.getBoolean(flag)
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt
git commit -m "feat(remote-config): register real-time listener in FeatureFlagManager"
```

---

## Task 3: Implement Real-Time Listener on Android

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt`

- [ ] **Step 1: Add necessary imports and implement `registerOnConfigUpdateListener`**

```kotlin
package com.programovil.aura.shared

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

class FirebaseRemoteConfigService(context: Context) : RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        val defaults = FeatureFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        remoteConfig.setDefaultsAsync(defaults)
    }

    override suspend fun getBoolean(flag: FeatureFlag): Boolean {
        return remoteConfig.getBoolean(flag.key)
    }

    override suspend fun getString(flag: FeatureFlag, default: String): String {
        return remoteConfig.getString(flag.key).takeIf { it.isNotEmpty() } ?: default
    }

    override suspend fun fetchAndActivate(): Result<Unit> = runCatching {
        remoteConfig.fetchAndActivate().await()
    }

    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    onUpdate()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                // Silently ignore; app continues with last known values
            }
        })
    }
}
```

- [ ] **Step 2: Verify Android compilation**

Run:
```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt
git commit -m "feat(remote-config): implement real-time config update listener on Android"
```

---

## Task 4: Add No-Op Listener to iOS Stub

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt`

- [ ] **Step 1: Add no-op `registerOnConfigUpdateListener`**

```kotlin
package com.programovil.aura.shared

class StubRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(flag: FeatureFlag): Boolean = flag.defaultValue
    override suspend fun getString(flag: FeatureFlag, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)

    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
        // No-op: iOS stub has no real-time updates
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt
git commit -m "feat(remote-config): add no-op config update listener to iOS stub"
```

---

## Task 5: Write `FeatureFlagManager` Unit Tests

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.programovil.aura.shared

import app.cash.turbine.test
import io.mockative.Mockable
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagManagerTest {

    private val mockService = mock(of<RemoteConfigService>())

    @Test
    fun `initialize fetches flags and emits defaults when all true`() = runTest {
        every { mockService.fetchAndActivate() } returns Result.success(Unit)
        every { mockService.registerOnConfigUpdateListener(any()) } returns Unit
        every { mockService.getBoolean(FeatureFlag.HABITS_ENABLED) } returns true
        every { mockService.getBoolean(FeatureFlag.TODOS_ENABLED) } returns true

        val manager = FeatureFlagManager(mockService)
        manager.initialize()

        manager.flags.test {
            val flags = awaitItem()
            assertTrue(flags[FeatureFlag.HABITS_ENABLED] == true)
            assertTrue(flags[FeatureFlag.TODOS_ENABLED] == true)
        }
    }

    @Test
    fun `listener callback refreshes flags and emits updated values`() = runTest {
        every { mockService.fetchAndActivate() } returns Result.success(Unit)

        var capturedCallback: (() -> Unit)? = null
        every { mockService.registerOnConfigUpdateListener(any()) } answers {
            capturedCallback = firstArg()
        }

        every { mockService.getBoolean(FeatureFlag.HABITS_ENABLED) } returns true
        every { mockService.getBoolean(FeatureFlag.TODOS_ENABLED) } returns true

        val manager = FeatureFlagManager(mockService)
        manager.initialize()

        // Update mock to return disabled after listener fires
        every { mockService.getBoolean(FeatureFlag.TODOS_ENABLED) } returns false

        manager.flags.test {
            // Skip initial emission
            awaitItem()

            // Fire the listener callback
            capturedCallback?.invoke()

            val updatedFlags = awaitItem()
            assertTrue(updatedFlags[FeatureFlag.HABITS_ENABLED] == true)
            assertFalse(updatedFlags[FeatureFlag.TODOS_ENABLED] == true)
        }
    }

    @Test
    fun `initialize handles fetch failure and still registers listener`() = runTest {
        every { mockService.fetchAndActivate() } returns Result.failure(Exception("network error"))
        every { mockService.registerOnConfigUpdateListener(any()) } returns Unit
        every { mockService.getBoolean(any()) } answers { arg<FeatureFlag>(0).defaultValue }

        val manager = FeatureFlagManager(mockService)
        manager.initialize()

        manager.flags.test {
            val flags = awaitItem()
            assertTrue(flags[FeatureFlag.HABITS_ENABLED] == true)
            assertTrue(flags[FeatureFlag.TODOS_ENABLED] == true)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail (compile errors expected due to new interface method)**

Run:
```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.FeatureFlagManagerTest"
```
Expected: Tests compile and pass (the interface already has the new method from Task 1)

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt
git commit -m "test(remote-config): add FeatureFlagManager unit tests"
```

---

## Task 6: Update `AppNavHost` to Guard Routes

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`

- [ ] **Step 1: Add `featureFlags` parameter and conditionally register routes**

```kotlin
package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.settings.presentation.screen.SettingsScreen
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    featureFlags: Map<FeatureFlag, Boolean>
) {
    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            val homeViewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                showTodos = featureFlags[FeatureFlag.TODOS_ENABLED] != false,
                showHabits = featureFlags[FeatureFlag.HABITS_ENABLED] != false,
                onTodoClick = {
                    if (featureFlags[FeatureFlag.TODOS_ENABLED] != false) {
                        navController.navigate(NavRoute.Todo)
                    }
                },
                onHabitClick = {
                    if (featureFlags[FeatureFlag.HABITS_ENABLED] != false) {
                        navController.navigate(NavRoute.Habit)
                    }
                },
                onSettingsClick = { navController.navigate(NavRoute.Settings) }
            )
        }

        if (featureFlags[FeatureFlag.TODOS_ENABLED] != false) {
            composable<NavRoute.Todo> {
                TodoScreen(
                    viewModel = todoViewModel,
                    featureFlags = featureFlags,
                    onFeatureDisabled = {
                        navController.popBackStack(NavRoute.Home, inclusive = false)
                    }
                )
            }
        }

        if (featureFlags[FeatureFlag.HABITS_ENABLED] != false) {
            composable<NavRoute.Habit> {
                HabitScreen(
                    featureFlags = featureFlags,
                    onFeatureDisabled = {
                        navController.popBackStack(NavRoute.Home, inclusive = false)
                    }
                )
            }
        }

        composable<NavRoute.Settings> {
            val settingsViewModel = koinViewModel<SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange,
                onSignOut = onSignOut
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt
git commit -m "feat(remote-config): guard NavHost routes with feature flags"
```

---

## Task 7: Update `HomeScreen` for Conditional Cards

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt`

- [ ] **Step 1: Add `showTodos` and `showHabits` parameters and conditionally render cards**

```kotlin
package com.programovil.aura.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.home.presentation.composable.DashboardCard
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.app_name_label
import aura_app.composeapp.generated.resources.home_dashboard_todos_title
import aura_app.composeapp.generated.resources.home_dashboard_habits_title
import aura_app.composeapp.generated.resources.home_dashboard_todos_subtitle
import aura_app.composeapp.generated.resources.home_dashboard_habits_subtitle
import aura_app.composeapp.generated.resources.settings_content_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    showTodos: Boolean = true,
    showHabits: Boolean = true,
    onTodoClick: () -> Unit = {},
    onHabitClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.background,
                        AppTheme.colors.surface
                    )
                )
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.app_name_label),
                style = AppTheme.typography.headlineLarge,
                color = AppTheme.colors.textPrimary
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(Res.string.settings_content_description),
                    tint = AppTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (showTodos) {
            DashboardCard(
                title = stringResource(Res.string.home_dashboard_todos_title),
                value = if (uiState.isLoading) "..." else uiState.dashboardData.incompleteTodos.toString(),
                subtitle = if (uiState.isLoading) "" else stringResource(Res.string.home_dashboard_todos_subtitle, uiState.dashboardData.incompleteTodos),
                onClick = onTodoClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showHabits) {
            DashboardCard(
                title = stringResource(Res.string.home_dashboard_habits_title),
                value = if (uiState.isLoading) "..." else uiState.dashboardData.currentStreak.toString(),
                subtitle = if (uiState.isLoading) "" else stringResource(Res.string.home_dashboard_habits_subtitle, uiState.dashboardData.completedHabitsToday, uiState.dashboardData.totalHabitsToday),
                onClick = onHabitClick
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt
git commit -m "feat(remote-config): conditionally show Home dashboard cards based on feature flags"
```

---

## Task 8: Update `TodoScreen` to Pop Back When Disabled

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 1: Read the current TodoScreen to understand its signature**

Read: `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`

- [ ] **Step 2: Add `featureFlags` and `onFeatureDisabled` parameters with `LaunchedEffect`**

Add inside the `@Composable` body (before any existing UI logic):

```kotlin
import androidx.compose.runtime.LaunchedEffect
import com.programovil.aura.shared.FeatureFlag

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {}
) {
    LaunchedEffect(featureFlags) {
        if (featureFlags[FeatureFlag.TODOS_ENABLED] == false) {
            onFeatureDisabled()
        }
    }
    // ... existing screen content
}
```

The exact placement depends on the current file content. The `LaunchedEffect` should be the first statement inside the `TodoScreen` composable, before any `Column`, `Box`, or other layout.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt
git commit -m "feat(remote-config): auto-pop TodoScreen when feature is disabled"
```

---

## Task 9: Update `HabitScreen` to Pop Back When Disabled

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`

- [ ] **Step 1: Read the current HabitScreen to understand its signature**

Read: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`

- [ ] **Step 2: Add `featureFlags` and `onFeatureDisabled` parameters with `LaunchedEffect`**

Add inside the `@Composable` body:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import com.programovil.aura.shared.FeatureFlag

@Composable
fun HabitScreen(
    featureFlags: Map<FeatureFlag, Boolean> = emptyMap(),
    onFeatureDisabled: () -> Unit = {}
) {
    LaunchedEffect(featureFlags) {
        if (featureFlags[FeatureFlag.HABITS_ENABLED] == false) {
            onFeatureDisabled()
        }
    }
    // ... existing screen content
}
```

The `LaunchedEffect` should be the first statement inside the `HabitScreen` composable.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt
git commit -m "feat(remote-config): auto-pop HabitScreen when feature is disabled"
```

---

## Task 10: Wire Everything in `App.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Pass `featureFlags` into `AppNavHost`**

Inside `AuthenticatedApp`, find the `AppNavHost` call and add the `featureFlags` parameter:

```kotlin
AppNavHost(
    navController = navController,
    todoViewModel = todoViewModel,
    currentThemeMode = currentThemeMode,
    onThemeChange = onThemeChange,
    onSignOut = onSignOut,
    featureFlags = featureFlags // ADD THIS
)
```

The `featureFlags` variable is already available in `AuthenticatedApp` via:
```kotlin
val featureFlags by featureFlagManager.flags.collectAsState()
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "feat(remote-config): pass feature flags into AppNavHost"
```

---

## Task 11: Full Build Verification

- [ ] **Step 1: Run common tests**

```bash
./gradlew :composeApp:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL (all tests pass)

- [ ] **Step 2: Verify Android compilation**

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit if any uncommitted changes exist**

```bash
git status
git add -A
git commit -m "feat(remote-config): complete real-time feature flag hot-reload"
```

---

## Self-Review Checklist

### Spec Coverage
- [x] `RemoteConfigService` interface gets `registerOnConfigUpdateListener` — Task 1
- [x] `FeatureFlagManager` registers listener and refreshes flags — Task 2
- [x] Android `FirebaseRemoteConfigService` implements real-time listener — Task 3
- [x] iOS `StubRemoteConfigService` gets no-op listener — Task 4
- [x] `AppNavHost` conditionally registers routes — Task 6
- [x] `HomeScreen` conditionally shows cards — Task 7
- [x] `TodoScreen` pops back when disabled — Task 8
- [x] `HabitScreen` pops back when disabled — Task 9
- [x] `App.kt` wires `featureFlags` into `AppNavHost` — Task 10
- [x] Unit tests for `FeatureFlagManager` — Task 5

### Placeholder Scan
- [x] No "TBD", "TODO", "implement later"
- [x] All steps contain actual code or exact commands
- [x] No vague references like "similar to Task N"

### Type Consistency
- [x] `registerOnConfigUpdateListener` signature matches across interface, Android impl, iOS stub
- [x] `FeatureFlagManager.refreshFlags()` uses same types as `_flags` StateFlow
- [x] `featureFlags: Map<FeatureFlag, Boolean>` used consistently in all UI layers
- [x] `onFeatureDisabled: () -> Unit` used consistently in both feature screens
