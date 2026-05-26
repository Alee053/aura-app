# Real-Time Feature Flag System Design

**Date:** 2026-05-25  
**Scope:** Remote Config hot-reload for feature toggles (Habits, Todos)  
**Approach:** Firebase Real-Time Listener (Android) + Reactive StateFlow propagation  

---

## 1. Goals

- Eliminate the need for a full app restart to apply Remote Config changes.
- Ensure feature flags (boolean only) propagate to the UI reactively within seconds of a Firebase Console publish.
- Guard all entry points to disabled features: bottom navigation, NavHost routes, and Home dashboard cards.
- Gracefully handle the edge case where a user is actively on a screen that gets disabled.

## 2. Architecture & Data Flow

```
Firebase Remote Config Console
         │
         │ (publish change)
         ▼
┌─────────────────────────────┐
│ Firebase Real-Time Listener │  ← Android only
│  (addOnConfigUpdateListener)│
└─────────────────────────────┘
         │
         │ (ConfigUpdate + activate)
         ▼
┌─────────────────────────────┐
│    RemoteConfigService      │  ← interface (commonMain)
│  ├─ registerListener()      │
│  └─ fetchAndActivate()      │
└─────────────────────────────┘
         │
         │ (callback)
         ▼
┌─────────────────────────────┐
│    FeatureFlagManager       │  ← commonMain
│  ├─ flags: StateFlow        │
│  └─ initialize()            │
└─────────────────────────────┘
         │
         │ (collectAsState)
         ▼
┌─────────────────────────────┐
│        App.kt               │
│  ├─ Bottom Nav (show/hide)│
│  ├─ NavHost (guard routes)  │
│  └─ Home cards (show/hide)  │
└─────────────────────────────┘
```

**iOS path:** `StubRemoteConfigService` returns defaults and registers a no-op listener. Feature flags remain at default values for the session. This is consistent with the current stub behavior and is acceptable because Firebase Remote Config iOS SDK integration is not yet wired.

## 3. Interface Changes

### 3.1 `RemoteConfigService` (commonMain)

Add a listener registration method so `FeatureFlagManager` can subscribe to real-time updates:

```kotlin
package com.programovil.aura.shared

interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun getString(flag: FeatureFlag, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>

    /** Register a callback that is invoked when remote config values change. */
    fun registerOnConfigUpdateListener(onUpdate: () -> Unit)
}
```

### 3.2 `FeatureFlagManager` (commonMain)

`initialize()` now registers the listener instead of performing a one-shot fetch. The listener callback re-reads all flags and updates `_flags` StateFlow.

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

The public API (`flags: StateFlow<Map<FeatureFlag, Boolean>>`) does not change, so all existing consumers in `App.kt` and elsewhere require no modifications.

### 3.3 `FirebaseRemoteConfigService` (androidMain)

Implements `registerOnConfigUpdateListener` using `Firebase.remoteConfig.addOnConfigUpdateListener`. When a `ConfigUpdate` fires, we call `activate()` and then invoke the app-side callback.

```kotlin
package com.programovil.aura.shared

import android.content.Context
import com.google.firebase.Firebase
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
            override fun onUpdate(configUpdate: com.google.firebase.remoteconfig.ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    onUpdate()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                // TODO: wire to a logger (non-fatal, silently ignore)
            }
        })
    }
}
```

> **Note:** `minimumFetchIntervalInSeconds = 0` is kept for development. For production, this should be raised to the Firebase default (12 hours) or an app-specific value.

### 3.4 `StubRemoteConfigService` (iosMain)

No-op listener. iOS stub has no real-time updates and returns defaults for the entire session.

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

## 4. UI Integration

### 4.1 Bottom Navigation (App.kt)

No changes required. The existing `featureFlags` StateFlow collection already reactively shows/hides the Todos and Habits tabs:

```kotlin
val featureFlags by featureFlagManager.flags.collectAsState()

val showTodos by remember(featureFlags) {
    mutableStateOf(featureFlags[FeatureFlag.TODOS_ENABLED] ?: true)
}
val showHabits by remember(featureFlags) {
    mutableStateOf(featureFlags[FeatureFlag.HABITS_ENABLED] ?: true)
}
```

### 4.2 NavHost Route Guarding (AppNavHost.kt)

Currently all routes are unconditionally registered. A dashboard card click or deep link can navigate to a disabled feature. Routes must be conditionally included, and click handlers must check the flag before navigating.

Changes to `AppNavHost` signature and body:

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    featureFlags: Map<FeatureFlag, Boolean> // NEW parameter
) {
    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            val homeViewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
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
                TodoScreen(viewModel = todoViewModel)
            }
        }

        if (featureFlags[FeatureFlag.HABITS_ENABLED] != false) {
            composable<NavRoute.Habit> {
                HabitScreen()
            }
        }

        composable<NavRoute.Settings> {
            // ... always available
        }
    }
}
```

### 4.3 Home Dashboard Cards (HomeScreen.kt)

The `HomeScreen` currently receives `onTodoClick` and `onHabitClick` lambdas. It should also receive the feature flags (or booleans) to conditionally render the Todo and Habit cards. If a feature is disabled, the card is not shown at all.

The `HomeViewModel` does not need to know about feature flags; the decision to show/hide cards is a pure UI concern. Pass `featureFlags` down from `AppNavHost` → `HomeScreen` → card visibility.

### 4.4 Edge Case: Active Screen Gets Disabled

**Chosen approach: Option 1 — pop user back to Home.**

If a user is on the Todo screen and an admin disables `TODOS_ENABLED`, the bottom nav tab disappears and the route is unregistered. To avoid leaving the user stranded on a ghost screen, each feature screen observes its flag and pops back when disabled.

Implementation pattern (applied to `TodoScreen` and `HabitScreen`):

```kotlin
@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    featureFlags: Map<FeatureFlag, Boolean>,
    onFeatureDisabled: () -> Unit
) {
    LaunchedEffect(featureFlags) {
        if (featureFlags[FeatureFlag.TODOS_ENABLED] == false) {
            onFeatureDisabled()
        }
    }
    // ... rest of screen
}
```

In `AppNavHost`, provide `onFeatureDisabled = { navController.popBackStack(NavRoute.Home, inclusive = false) }`.

## 5. Error Handling

| Scenario | Behavior |
|----------|----------|
| `fetchAndActivate()` fails on startup | `FeatureFlagManager` falls back to defaults already in `_flags`. Listener registration still proceeds so future network recovery can pick up updates. |
| Real-time listener `onError` fires | Silently ignored (logged if logger available). The app continues with the last known flag values. |
| User offline at startup | Same as failure: defaults are used. Listener is registered; when connectivity returns, Firebase will deliver the update. |
| iOS stub | Always returns defaults. No errors possible. |

## 6. Testing

### 6.1 Unit Tests (commonTest)

- **`FeatureFlagManagerTest`**: Verify that when `registerOnConfigUpdateListener` callback is invoked, `flags` StateFlow emits the updated map.
- Mock `RemoteConfigService` using Mockative (`@Mockable`).

```kotlin
@Test
fun `when listener callback fires, flags flow updates`() = runTest {
    val manager = FeatureFlagManager(mockService)

    manager.initialize()
    // capture the registered callback
    val callback = /* captured from mock */

    every { mockService.getBoolean(FeatureFlag.TODOS_ENABLED) } returns false

    callback()

    manager.flags.test {
        val latest = awaitItem()
        assertEquals(false, latest[FeatureFlag.TODOS_ENABLED])
    }
}
```

### 6.2 Android Instrumented Tests

- Verify that changing a Remote Config value in the Firebase Console (or via test API) causes the bottom nav tab to disappear within a reasonable timeout.

## 7. Rollout & Risks

| Risk | Mitigation |
|------|------------|
| Real-time listener fires too aggressively in development | `minimumFetchIntervalInSeconds = 0` is already set; this is expected during active development. |
| NavHost conditional routes cause navigation crashes if a deep link targets a disabled feature | Deep links to disabled routes will hit an unregistered route. Compose Navigation handles this gracefully by staying on the current destination. If deep links become a production concern, add an explicit "Feature Unavailable" fallback screen. |
| iOS users never see real-time updates | Acceptable for now. iOS Firebase SDK integration is out of scope. When integrated, `StubRemoteConfigService` is replaced with a real iOS implementation using the same interface. |

## 8. Future Extensions

- Add new `FeatureFlag` enum entries for future features (e.g., `NOTIFICATIONS_ENABLED`). No interface changes required.
- Extend `RemoteConfigService` with `getLong()`, `getDouble()`, or `getJson()` when non-boolean remote config values are needed.
- Wire iOS `FIRRemoteConfig` real-time listener once the iOS Firebase module is integrated.
