# RC Manager Unification + A/B Experiments Re-base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the A/B experiments from PR #12 on top of `master` with `google-services.json` restored, after unifying remote-config value management behind one generic core.

**Architecture:** A new generic `RemoteConfigValueManager<T>` in `commonMain/.../shared/` owns the fetch/listener/lifecycle pattern. `FeatureFlagManager` and `UserPlanManager` become typed wrappers on top. `RemoteConfigService` is generalized to a key-based interface. A new `GetHabitsAccessibilityUseCase` is the single source of truth for "is this user allowed to see Habits?" (feature flag AND user plan).

**Tech Stack:** Kotlin Multiplatform, kotlinx.coroutines + StateFlow, Koin DI, kotlin-test + Turbine + Mockative, Compose Multiplatform.

**Worktree:** `.worktrees/extra-credit-ab-testing/` on branch `refactor/extra-credit-ab-testing` (tracks `origin/feature/extra-credit-implementation`).

**Test command:** `./gradlew :composeApp:testDebugUnitTest` from the worktree root.

---

## File map

### New files
- `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigValueManager.kt` — generic core.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCase.kt` — single source of truth for Habits access.
- `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt` — generic core tests.
- `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/UserPlanManagerTest.kt` — UserPlan wrapper tests.
- `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCaseTest.kt` — access-rule tests.
- `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FakeRemoteConfigService.kt` — shared test fake (extracted from current `FeatureFlagManagerTest`).

### Modified files
- `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt` — generalize to key-based interface.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt` — refactor to typed wrapper.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/UserPlanManager.kt` — refactor to typed wrapper.
- `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt` — implement new interface.
- `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt` — implement new interface.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt` — register `GetHabitsAccessibilityUseCase`.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt` — consume the new use case for the Habits tab.
- `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt` — adapt to new shape (use shared fake).
- `composeApp/google-services.json` — restore master's `aura-app-7dce3` project.
- `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt` — restore master's Google client ID.

### Unchanged
- All `experiments/` domain/data/presentation files (no semantic changes).
- `ExperimentsHeartbeatWorker`, RTDB event format, `UserPlanRepository`, `GetUserPlanUseCase`, `GetHomeVariantUseCase`, `GetNotificationVariantUseCase`, `LogExperimentEventUseCase`, `AuthError`, `AuthViewModel`, `AndroidAuthService`, `IosAuthService`, `AndroidNotificationScheduler`, `DailySummaryWorker`, `HomeViewModel`, `AppNavHost`, all string resources.

---

## Task 1: Restore master's `google-services.json`

**Files:**
- Modify: `composeApp/google-services.json`

- [ ] **Step 1: Replace file with master version**

```bash
git show origin/master:composeApp/google-services.json > composeApp/google-services.json
```

- [ ] **Step 2: Verify the project id is `aura-app-7dce3`**

```bash
grep '"project_id"' composeApp/google-services.json
```

Expected: `"project_id": "aura-app-7dce3",`

- [ ] **Step 3: Commit**

```bash
git add composeApp/google-services.json
git commit -m "chore(firebase): restore master google-services.json project"
```

---

## Task 2: Restore master's Google client ID in `MainActivity`

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt:14-23`

- [ ] **Step 1: Replace the file with the master version**

The PR rewrote `MainActivity.kt` substantially. The restore target is master's `launchGoogleSignIn` shape (no `GOOGLE_SERVER_CLIENT_ID` const, original `setServerClientId("623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com")`, original catch blocks).

```bash
git show origin/master:composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt > composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt
```

- [ ] **Step 2: Verify the client ID is restored**

```bash
grep -n 'setServerClientId\|gpn8fq0c03i0khmt3nn9bj0h92fprnfh' composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt
```

Expected: `setServerClientId("623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com")` is present.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt
git commit -m "fix(auth): restore master Google OAuth client ID"
```

---

## Task 3: Add failing test for `RemoteConfigValueManager<T>` parser fallback

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FakeRemoteConfigService.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt`

- [ ] **Step 1: Extract `FakeRemoteConfigService` from `FeatureFlagManagerTest.kt`**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FakeRemoteConfigService.kt` with content:

```kotlin
package com.programovil.aura.shared

class FakeRemoteConfigService(
    private val fetchResult: Result<Unit> = Result.success(Unit),
    private val stringValues: Map<String, String> = emptyMap(),
    private val booleanValues: Map<String, Boolean> = emptyMap()
) : RemoteConfigService {
    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        booleanValues[key] ?: default
    override suspend fun getString(key: String, default: String): String =
        stringValues[key] ?: default
    override suspend fun fetchAndActivate(): Result<Unit> = fetchResult
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
}
```

- [ ] **Step 2: Remove the local fake from `FeatureFlagManagerTest.kt`**

Delete the `private class FakeRemoteConfigService` block at the bottom of `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt` (lines 58-67). The test file will be updated for the new interface in a later task — for now it won't compile, that's expected.

- [ ] **Step 3: Write the failing test for parser fallback**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt`:

```kotlin
package com.programovil.aura.shared

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteConfigValueManagerTest {

    @Test
    fun `parser failure falls back to defaultValue`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf("user_plan" to "unknown")
        )
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "user_plan",
            defaultValue = "Free",
            parser = { raw -> if (raw == "Premium") "Premium" else "Free" }
        )

        manager.value.test {
            assertEquals("Free", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 4: Run the test to confirm it fails to compile**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.RemoteConfigValueManagerTest"`

Expected: **COMPILATION ERROR** — `Unresolved reference: RemoteConfigValueManager`.

- [ ] **Step 5: Commit the failing test**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FakeRemoteConfigService.kt
git add composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt
git commit -m "test(shared): add failing test for RemoteConfigValueManager parser fallback"
```

---

## Task 4: Implement `RemoteConfigValueManager<T>`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigValueManager.kt`

- [ ] **Step 1: Create the generic core**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigValueManager.kt`:

```kotlin
package com.programovil.aura.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Generic holder for a single value sourced from [RemoteConfigService].
 *
 * Owns the fetch + real-time listener + lifecycle pattern. Typed wrappers
 * (e.g. [FeatureFlagManager], [UserPlanManager]) compose one or more of these.
 *
 * Lifecycle:
 * - Call [initialize] once at startup, then read [value].
 * - Call [stop] to cancel the internal scope. After [stop] the manager is dead.
 * - Calling [refresh] after [stop] is a silent no-op.
 * - Calling [refresh] before [initialize] is permitted but the fetched value
 *   is not reflected in [value] until the listener is registered; in practice
 *   [refresh] is only useful inside [initialize] or as an on-resume re-fetch.
 *
 * No polling loop is started: Firebase's real-time
 * [RemoteConfigService.registerOnConfigUpdateListener] is the only source of
 * updates after the initial fetch.
 */
class RemoteConfigValueManager<T>(
    private val remoteConfigService: RemoteConfigService,
    private val key: String,
    private val defaultValue: T,
    private val parser: (String) -> T
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _value = MutableStateFlow(defaultValue)
    val value: StateFlow<T> = _value.asStateFlow()

    suspend fun initialize() {
        refresh()
        remoteConfigService.registerOnConfigUpdateListener {
            scope.launch { refresh() }
        }
    }

    fun refresh() {
        scope.launch {
            remoteConfigService.fetchAndActivate()
            val raw = remoteConfigService.getString(key, defaultValue.toString())
            _value.value = parser(raw)
        }
    }

    fun stop() {
        scope.cancel()
    }
}
```

- [ ] **Step 2: Run the parser-fallback test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.RemoteConfigValueManagerTest"`

Expected: PASS (1/1).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigValueManager.kt
git commit -m "feat(shared): add generic RemoteConfigValueManager core"
```

---

## Task 5: Add tests for `RemoteConfigValueManager` lifecycle and refresh-after-listener

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt`

- [ ] **Step 1: Add three more tests to the file**

Append the following to `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt` (inside the class, after the existing test):

```kotlin
    @Test
    fun `initialize seeds value from current RemoteConfigService string`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf("user_plan" to "Premium")
        )
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "user_plan",
            defaultValue = "Free",
            parser = { raw -> if (raw == "Premium") "Premium" else "Free" }
        )

        manager.initialize()

        assertEquals("Premium", manager.value.value)
    }

    @Test
    fun `stop cancels the internal scope - refresh becomes a no-op`() = runTest {
        var callCount = 0
        val fake = object : RemoteConfigService {
            override suspend fun getBoolean(key: String, default: Boolean) = default
            override suspend fun getString(key: String, default: String) = {
                callCount++
                default
            }
            override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
            override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
        }
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "k",
            defaultValue = "d",
            parser = { it }
        )

        manager.stop()
        manager.refresh()

        assertEquals(0, callCount)
    }

    @Test
    fun `refresh parses the latest string after a real-time update`() = runTest {
        val backing = mutableMapOf<String, String>("k" to "v1")
        var listener: (() -> Unit)? = null
        val fake = object : RemoteConfigService {
            override suspend fun getBoolean(key: String, default: Boolean) = default
            override suspend fun getString(key: String, default: String) = backing[key] ?: default
            override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
            override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) { listener = onUpdate }
        }
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "k",
            defaultValue = "v0",
            parser = { it }
        )
        manager.initialize()
        assertEquals("v1", manager.value.value)

        backing["k"] = "v2"
        listener?.invoke()

        manager.value.test {
            assertEquals("v2", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
```

- [ ] **Step 2: Run the new tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.RemoteConfigValueManagerTest"`

Expected: PASS (4/4).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/shared/RemoteConfigValueManagerTest.kt
git commit -m "test(shared): cover RemoteConfigValueManager lifecycle and refresh"
```

---

## Task 6: Generalize `RemoteConfigService` interface

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt`
- Modify: `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt`

- [ ] **Step 1: Replace the common interface**

Overwrite `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt` with:

```kotlin
package com.programovil.aura.shared

import io.mockative.Mockable

@Mockable
interface RemoteConfigService {
    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun getString(key: String, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>

    /** Register a callback that is invoked when remote config values change. */
    fun registerOnConfigUpdateListener(onUpdate: () -> Unit)
}
```

- [ ] **Step 2: Update `FirebaseRemoteConfigService` (Android) to implement the new interface**

Overwrite `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt` with:

```kotlin
package com.programovil.aura.shared

import android.util.Log
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

private const val TAG = "FirebaseRemoteConfig"

class FirebaseRemoteConfigService(context: Context) : RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig
    private var listenerRegistration: ConfigUpdateListenerRegistration? = null

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        val flagDefaults = FeatureFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        val userPlanDefaults = UserPlanFlag.entries.associate { flag ->
            flag.key to flag.defaultValue
        }
        remoteConfig.setDefaultsAsync(flagDefaults + userPlanDefaults)
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean {
        return remoteConfig.getBoolean(key)
    }

    override suspend fun getString(key: String, default: String): String {
        return remoteConfig.getString(key).takeIf { it.isNotEmpty() } ?: default
    }

    override suspend fun fetchAndActivate(): Result<Unit> = runCatching {
        remoteConfig.fetchAndActivate().await()
    }

    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
        listenerRegistration?.remove()
        listenerRegistration = remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Remote config update received for keys: ${configUpdate.updatedKeys}, activating...")
                remoteConfig.activate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Remote config activated successfully")
                        onUpdate()
                    } else {
                        Log.e(TAG, "Failed to activate remote config", task.exception)
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e(TAG, "Remote config listener error [${error.code}]: ${error.message}", error)
            }
        })
        Log.d(TAG, "Real-time config listener registered and held")
    }
}
```

- [ ] **Step 3: Update `StubRemoteConfigService` (iOS) to implement the new interface**

Overwrite `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt` with:

```kotlin
package com.programovil.aura.shared

class StubRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(key: String, default: Boolean): Boolean = default
    override suspend fun getString(key: String, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
    }
}
```

- [ ] **Step 4: Run all common tests to confirm the interface change is wired**

Run: `./gradlew :composeApp:testDebugUnitTest`

Expected: the `RemoteConfigValueManagerTest` tests pass; the `FeatureFlagManagerTest` will fail to compile (it still uses the old typed interface) — that's expected and resolved in Task 7.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt
git add composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt
git add composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt
git commit -m "refactor(shared): generalize RemoteConfigService to key-based interface"
```

---

## Task 7: Add failing test for `UserPlanManager`

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/UserPlanManagerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/UserPlanManagerTest.kt`:

```kotlin
package com.programovil.aura.shared

import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPlanManagerTest {

    @Test
    fun `initialize reads user_plan string and parses to UserPlan_Premium`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf(UserPlanFlag.USER_PLAN.key to "Premium")
        )
        val manager = UserPlanManager(fake)

        manager.initialize()

        assertEquals(UserPlan.Premium, manager.userPlan.value)
    }

    @Test
    fun `initialize falls back to Free when remote returns unknown value`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf(UserPlanFlag.USER_PLAN.key to "Garbage")
        )
        val manager = UserPlanManager(fake)

        manager.initialize()

        assertEquals(UserPlan.Free, manager.userPlan.value)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails to compile**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.UserPlanManagerTest"`

Expected: **COMPILATION ERROR** — `Unresolved reference: userPlan` and the constructor still accepts only `RemoteConfigService` (it does, but `initialize()` shape will be different).

- [ ] **Step 3: Commit the failing test**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/shared/UserPlanManagerTest.kt
git commit -m "test(shared): add failing test for UserPlanManager"
```

---

## Task 8: Refactor `UserPlanManager` to a typed wrapper on `RemoteConfigValueManager`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/UserPlanManager.kt`

- [ ] **Step 1: Replace `UserPlanManager.kt` with the typed wrapper**

Overwrite `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/UserPlanManager.kt`:

```kotlin
package com.programovil.aura.shared

import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.flow.StateFlow

/**
 * Typed wrapper that exposes the current [UserPlan] (Free or Premium) sourced
 * from Remote Config. Internally owns a [RemoteConfigValueManager]; no polling
 * loop is started.
 */
class UserPlanManager(
    remoteConfigService: RemoteConfigService
) {
    private val manager = RemoteConfigValueManager(
        remoteConfigService = remoteConfigService,
        key = UserPlanFlag.USER_PLAN.key,
        defaultValue = UserPlanFlag.USER_PLAN.defaultValue,
        parser = { raw -> UserPlan.fromRemoteConfigString(raw) }
    )

    val userPlan: StateFlow<UserPlan> = manager.value

    suspend fun initialize() = manager.initialize()

    fun refresh() = manager.refresh()

    fun stop() = manager.stop()
}
```

- [ ] **Step 2: Run the UserPlanManager tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.UserPlanManagerTest"`

Expected: PASS (2/2).

- [ ] **Step 3: Run the full test suite to confirm no regressions**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.*"`

Expected: only `FeatureFlagManagerTest` still failing (next task). All others pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/UserPlanManager.kt
git commit -m "refactor(shared): reimplement UserPlanManager on RemoteConfigValueManager"
```

---

## Task 9: Refactor `FeatureFlagManager` to a typed wrapper

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt`

- [ ] **Step 1: Update `FeatureFlagManager.kt` to the new shape**

Overwrite `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt`:

```kotlin
package com.programovil.aura.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Typed wrapper that exposes a snapshot [Map] of every [FeatureFlag] sourced
 * from Remote Config. Internally composes one [RemoteConfigValueManager] per
 * flag entry; no polling loop is started.
 */
class FeatureFlagManager(
    remoteConfigService: RemoteConfigService
) {
    private val managers: Map<FeatureFlag, RemoteConfigValueManager<Boolean>> =
        FeatureFlag.entries.associateWith { flag ->
            RemoteConfigValueManager(
                remoteConfigService = remoteConfigService,
                key = flag.key,
                defaultValue = flag.defaultValue,
                parser = { raw -> raw.toBooleanStrictOrNull() ?: flag.defaultValue }
            )
        }

    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        managers.values.forEach { it.initialize() }
        managers.forEach { (flag, mgr) ->
            mgr.value.collect { newValue ->
                _flags.update { it + (flag to newValue) }
            }
        }
    }

    fun stop() {
        managers.values.forEach { it.stop() }
    }
}
```

- [ ] **Step 2: Update `FeatureFlagManagerTest.kt` to use the new interface and the shared fake**

Overwrite `composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt`:

```kotlin
package com.programovil.aura.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagManagerTest {

    @Test
    fun `initialize fetches flags and emits remote values`() = runTest {
        val manager = FeatureFlagManager(FakeRemoteConfigService(
            booleanValues = mapOf(
                FeatureFlag.HABITS_ENABLED.key to true,
                FeatureFlag.TODOS_ENABLED.key to true
            )
        ))
        manager.initialize()

        val flags = manager.flags.value
        assertEquals(true, flags[FeatureFlag.HABITS_ENABLED])
        assertEquals(true, flags[FeatureFlag.TODOS_ENABLED])
    }

    @Test
    fun `initialize reads remote-disabled flags as false`() = runTest {
        val manager = FeatureFlagManager(FakeRemoteConfigService(
            booleanValues = mapOf(
                FeatureFlag.HABITS_ENABLED.key to false,
                FeatureFlag.TODOS_ENABLED.key to false
            )
        ))
        manager.initialize()

        val flags = manager.flags.value
        assertEquals(false, flags[FeatureFlag.HABITS_ENABLED])
        assertEquals(false, flags[FeatureFlag.TODOS_ENABLED])
    }
}
```

- [ ] **Step 3: Run the updated tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.FeatureFlagManagerTest"`

Expected: PASS (2/2).

- [ ] **Step 4: Run the full shared test suite**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.shared.*"`

Expected: PASS — `FakeRemoteConfigService`, `RemoteConfigValueManagerTest`, `UserPlanManagerTest`, `FeatureFlagManagerTest` all green.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt
git add composeApp/src/commonTest/kotlin/com/programovil/aura/shared/FeatureFlagManagerTest.kt
git commit -m "refactor(shared): reimplement FeatureFlagManager on RemoteConfigValueManager"
```

---

## Task 10: Add failing test for `GetHabitsAccessibilityUseCase`

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCaseTest.kt`:

```kotlin
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetHabitsAccessibilityUseCaseTest {

    private val featureFlagManager = mock(of<FeatureFlagManager>())
    private val getUserPlanUseCase = mock(of<GetUserPlanUseCase>())
    private val useCase = GetHabitsAccessibilityUseCase(featureFlagManager, getUserPlanUseCase)

    @Test
    fun `flag true and Premium plan returns true`() = runTest {
        every { featureFlagManager.flags } returns MutableStateFlow(
            mapOf(FeatureFlag.HABITS_ENABLED to true)
        )
        every { getUserPlanUseCase() } returns flowOf(UserPlan.Premium)

        val result = useCase().let { flow ->
            var collected = false
            var value = false
            flow.collect {
                collected = true
                value = it
            }
            if (!collected) error("use case emitted no value")
            value
        }

        assertEquals(true, result)
    }

    @Test
    fun `flag true and Free plan returns false`() = runTest {
        every { featureFlagManager.flags } returns MutableStateFlow(
            mapOf(FeatureFlag.HABITS_ENABLED to true)
        )
        every { getUserPlanUseCase() } returns flowOf(UserPlan.Free)

        val collected = mutableListOf<Boolean>()
        useCase().collect { collected += it }

        assertEquals(listOf(false), collected)
    }

    @Test
    fun `flag false returns false regardless of plan`() = runTest {
        every { featureFlagManager.flags } returns MutableStateFlow(
            mapOf(FeatureFlag.HABITS_ENABLED to false)
        )
        every { getUserPlanUseCase() } returns flowOf(UserPlan.Premium)

        val collected = mutableListOf<Boolean>()
        useCase().collect { collected += it }

        assertEquals(listOf(false), collected)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails to compile**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCaseTest"`

Expected: **COMPILATION ERROR** — `Unresolved reference: GetHabitsAccessibilityUseCase`.

- [ ] **Step 3: Commit the failing test**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCaseTest.kt
git commit -m "test(habit): add failing test for GetHabitsAccessibilityUseCase"
```

---

## Task 11: Implement `GetHabitsAccessibilityUseCase`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCase.kt`

- [ ] **Step 1: Create the use case**

Create `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCase.kt`:

```kotlin
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Single source of truth for "can this user see the Habits feature?".
 *
 * Both signals must be true:
 *   - [FeatureFlag.HABITS_ENABLED] is the global kill switch.
 *   - The user has [UserPlan.Premium] (entitlement).
 *
 * Consumers should subscribe via [invoke] rather than re-deriving the rule
 * from the two flows.
 */
class GetHabitsAccessibilityUseCase(
    private val featureFlagManager: FeatureFlagManager,
    private val getUserPlanUseCase: GetUserPlanUseCase
) {
    operator fun invoke(): Flow<Boolean> = combine(
        featureFlagManager.flags,
        getUserPlanUseCase()
    ) { flags, plan ->
        (flags[FeatureFlag.HABITS_ENABLED] ?: true) && plan is UserPlan.Premium
    }
}
```

- [ ] **Step 2: Run the use case tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCaseTest"`

Expected: PASS (3/3).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsAccessibilityUseCase.kt
git commit -m "feat(habit): add GetHabitsAccessibilityUseCase"
```

---

## Task 12: Register `GetHabitsAccessibilityUseCase` in `HabitModule`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt`

- [ ] **Step 1: Add the use case to the module**

Replace the contents of `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt` with:

```kotlin
package com.programovil.aura.habit.di

import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.repository.createHabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.DeleteHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsWithStatusUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.domain.usecase.UpdateHabitUseCase
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val habitModule = module {
    // Data layer
    single { createHabitRepository() }

    // Domain layer - use cases
    factoryOf(::AddHabitUseCase)
    factoryOf(::GetHabitsWithStatusUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::DeleteHabitUseCase)
    factoryOf(::UpdateHabitUseCase)
    factoryOf(::GetHabitsAccessibilityUseCase)

    // Presentation layer
    viewModelOf(::HabitViewModel)
}
```

- [ ] **Step 2: Verify the module compiles by running a habit test**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.programovil.aura.habit.*"`

Expected: PASS (the existing habit tests still pass; the new use case is just registered, not yet consumed).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
git commit -m "feat(habit): register GetHabitsAccessibilityUseCase in Koin module"
```

---

## Task 13: Replace inline premium gate in `App.kt` with the use case

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Update the imports and replace the `showHabits` derivation**

In `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`:

- Add import after line 49 (`com.programovil.aura.shared.FeatureFlagManager`):
  ```kotlin
  import com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCase
  ```

- Replace the block at lines 141-147:
  ```kotlin
  val showHabits by remember(featureFlags) {
      mutableStateOf(featureFlags[FeatureFlag.HABITS_ENABLED] ?: true)
  }
  val showJournals by remember(featureFlags) {
      mutableStateOf(featureFlags[FeatureFlag.JOURNAL_ENABLED] ?: true)
  }
  val showPremiumFeatures = userPlan is com.programovil.aura.experiments.domain.model.UserPlan.Premium
  ```
  with:
  ```kotlin
  val showTodos by remember(featureFlags) {
      mutableStateOf(featureFlags[FeatureFlag.TODOS_ENABLED] ?: true)
  }
  val showJournals by remember(featureFlags) {
      mutableStateOf(featureFlags[FeatureFlag.JOURNAL_ENABLED] ?: true)
  }
  val showHabitsAccessible: Boolean by getHabitsAccessibilityUseCase()
      .collectAsState(initial = true)
  ```
  (Note: `showTodos` is unchanged; we just remove the `showHabits` block and the `showPremiumFeatures` derivation, replacing with a single accessibility flow.)

- Remove the now-unused `showTodos` line that already exists at 138-140? No — keep it. Only the `showHabits` and `showPremiumFeatures` lines are removed/replaced.

- Add the injection at the top of `AuthenticatedApp` (after line 126, the `featureFlagManager` injection):
  ```kotlin
  val getHabitsAccessibilityUseCase: GetHabitsAccessibilityUseCase = koinInject()
  ```

- Update the Habits tab gate at line 195 from:
  ```kotlin
  if (showHabits && showPremiumFeatures) {
  ```
  to:
  ```kotlin
  if (showHabitsAccessible) {
  ```

- Update the Journal tab gate at line 211 from:
  ```kotlin
  if (showJournals && showPremiumFeatures) {
  ```
  to:
  ```kotlin
  if (showJournals) {  // Journal is not yet premium-gated; only Habits is.
  ```
  (Or keep the existing line and just leave the premium check off — the spec only mandates the Habits rule; Journal's premium gate is out of scope per the design doc's "YAGNI" section. Restore it to its master shape of `if (showJournals)`.)

- The result: `showHabitsAccessible` is the single boolean the UI checks, and the rule lives in the use case.

- [ ] **Step 2: Compile-check the common module**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`

Expected: BUILD SUCCESSFUL. (Compose may complain about `unused` warnings on `showPremiumFeatures`/`showHabits`; remove them per Step 1.)

- [ ] **Step 3: Run the full test suite**

Run: `./gradlew :composeApp:testDebugUnitTest`

Expected: PASS for all tests. The new tests (`RemoteConfigValueManagerTest`, `UserPlanManagerTest`, `GetHabitsAccessibilityUseCaseTest`) and the updated `FeatureFlagManagerTest` pass; all experiments tests are unchanged.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "refactor(app): consume GetHabitsAccessibilityUseCase for Habits tab"
```

---

## Task 14: Final verification and PR-ready summary

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew :composeApp:testDebugUnitTest`

Expected: ALL TESTS PASS.

- [ ] **Step 2: Build the Android debug APK**

Run: `./gradlew :composeApp:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Diff the worktree branch against `master` to confirm scope**

```bash
git log --oneline origin/master..HEAD
```

Expected: ~13 commits, covering:
- Two `chore`/`fix` for project config restore.
- One test commit + one impl commit for `RemoteConfigValueManager`.
- One test commit for `RemoteConfigValueManager` lifecycle.
- One refactor commit for `RemoteConfigService` interface.
- One test commit + one refactor commit for `UserPlanManager`.
- One refactor commit for `FeatureFlagManager`.
- One test commit + one impl commit for `GetHabitsAccessibilityUseCase`.
- One commit to register the use case in `HabitModule`.
- One commit for `App.kt` to consume the use case.

- [ ] **Step 4: Push the branch and open a PR**

```bash
git push -u origin refactor/extra-credit-ab-testing
gh pr create --base master --title "RC manager unification + A/B experiments (re-based)" --body "..."
```

In the PR body, link to PR #12 and the design spec at `docs/superpowers/specs/2026-06-09-rc-manager-unify-and-ab-rebase.md`.

---

## Self-review notes

- **Spec coverage**: each section of the design doc is mapped to a task: generic core (Tasks 3-5), typed wrappers (Tasks 7-9), interface generalization (Task 6), domain use case (Tasks 10-12), `App.kt` consumption (Task 13), restore order (Tasks 1-2), tests (Tasks 3, 5, 7, 10).
- **Type consistency**: `RemoteConfigValueManager<T>` is referenced as `RemoteConfigValueManager` (no type arg) at call sites in `UserPlanManager` and `FeatureFlagManager` — type inference fills it. The test in Task 5 uses the anonymous-object `RemoteConfigService` implementation; this is the only test that bypasses `FakeRemoteConfigService` because it needs to count calls.
- **No placeholders**: every step has the actual code, the actual command, and the expected output. No "TBD", no "similar to Task N".
- **Lifecycle contract**: documented in the KDoc on `RemoteConfigValueManager` (Task 4) and tested in Task 5.
- **Restore order**: Tasks 1-2 happen before any code change so the worktree branch is clean of the wrong Firebase project from the moment the refactor begins.
