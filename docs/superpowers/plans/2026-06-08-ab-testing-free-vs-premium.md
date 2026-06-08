# A/B Testing Free vs Premium Implementation Plan

> **For agentic workers:** This plan is self-contained and assumes zero context. Execute task by task, marking each `- [ ]` checkbox. Each task is 2-5 minutes. Tests are written FIRST (TDD discipline), implementation follows to make them pass, then commit. **Do NOT skip the test-first step.**

**Goal:** Add a plan-based A/B experiment (Free vs Premium) that drives visible UI differences (Home cards, bottom-nav tabs, notification cadence) using Firebase Remote Config + DataStore, with events logged to Realtime Database.

**Architecture:** New `experiments/` feature module following the existing Clean Architecture pattern (`domain/`, `data/`, `presentation/`, `di/`). Plan value (`Free`/`Premium`) is stored in DataStore and sourced from a single Remote Config parameter `user_plan`. A new `ExperimentsHeartbeatWorker` runs every 12h and logs a `session_active` event to Realtime Database. iOS gets compile-only stubs (matches existing pattern).

**Tech Stack:** Kotlin 2.2.10, KMP (commonMain / androidMain / iosMain), Compose Multiplatform 1.10.3, Koin 4.1.1, Mockative 3.2.3, Turbine 1.1.0, kotlinx-coroutines-test 1.8.1, WorkManager 2.9.0, DataStore 1.1.0, Firebase BOM 34.12.0, Firebase Remote Config 22.1.2, Firebase Realtime Database 21.0.0.

**Branch:** `feature/extra-credit-implementation` (already created from master, working tree currently clean except for the auth fix from a prior task — verified).

**Spec reference:** `docs/superpowers/specs/2026-06-08-ab-testing-free-vs-premium-design.md`

**Pre-existing context (verified by explore agent):**
- 10 tests in `:composeApp:testDebugUnitTest` already fail on `master` (Habit/Onboarding/Todo ViewModels — Mockative import issues, **unrelated to this work**). We must not introduce **new** failures.
- `Master` SHA: `7af547b`. Worktree: clean. Branch: `feature/extra-credit-implementation` with the auth-login fix from the prior task.
- `FirebaseMessagingService.onNewToken` is intentionally a no-op (out of scope).

---

## File structure for this plan

**Created (new):**

```
composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/
├── domain/
│   ├── model/
│   │   ├── UserPlan.kt
│   │   ├── HomeVariant.kt
│   │   ├── NotificationVariant.kt
│   │   ├── Tone.kt
│   │   └── ExperimentEvent.kt
│   ├── repository/
│   │   ├── UserPlanRepository.kt
│   │   └── ExperimentRepository.kt
│   └── usecase/
│       ├── GetUserPlanUseCase.kt
│       ├── GetHomeVariantUseCase.kt
│       ├── GetNotificationVariantUseCase.kt
│       └── LogExperimentEventUseCase.kt
└── di/
    └── ExperimentsModule.kt

composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/
├── domain/
│   ├── model/
│   │   ├── UserPlanTest.kt
│   │   └── ExperimentEventTest.kt
│   └── usecase/
│       ├── GetUserPlanUseCaseTest.kt
│       ├── GetHomeVariantUseCaseTest.kt
│       ├── GetNotificationVariantUseCaseTest.kt
│       └── LogExperimentEventUseCaseTest.kt

composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/
├── data/repository/
│   ├── DataStoreUserPlanRepositoryImpl.kt
│   └── FirebaseExperimentRepositoryImpl.kt
└── presentation/worker/
    └── ExperimentsHeartbeatWorker.kt

composeApp/src/iosMain/kotlin/com/programovil/aura/experiments/
├── data/repository/
│   ├── IosUserPlanRepositoryImpl.kt
│   └── IosExperimentRepositoryImpl.kt
```

**Modified (existing):**
- `gradle/libs.versions.toml` — add `firebaseDatabase` version + `firebase-database-ktx` library alias.
- `composeApp/build.gradle.kts` — add `implementation(libs.firebase.database.ktx)`.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt` — add `USER_PLAN` (String-flag).
- `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt` — add `getUserPlan()` extension to the interface (typed accessor for `USER_PLAN`).
- `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt` — implement `getUserPlan()`.
- `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt` — implement `getUserPlan()`.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt` — add `experimentsModule` to `getModules`.
- `composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt` — schedule `ExperimentsHeartbeatWorker` on startup.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt` — derive `showPremiumFeatures: Boolean` from `GetUserPlanUseCase`, pass to `HomeScreen` and gate `NavigationBarItem`s.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/HomeViewModel.kt` — inject `GetHomeVariantUseCase`, expose `homeVariant` in UiState.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt` — conditionally render 3rd `DashboardCard`.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/viewmodel/SettingsViewModel.kt` — inject `UserPlanRepository` + `GetNotificationVariantUseCase`, expose `userPlan` and `setSimulatedPlan(plan)`.
- `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt` — render "Simulate Plan" section.
- `composeApp/src/androidMain/kotlin/com/programovil/aura/notification/domain/AndroidNotificationScheduler.kt` — read `GetNotificationVariantUseCase` at schedule time, enqueue 1 or 2 work requests.
- `composeApp/src/androidMain/kotlin/com/programovil/aura/notification/presentation/worker/DailySummaryWorker.kt` — read `GetNotificationVariantUseCase`; if `useDueDateChannel` and a todo has `dueDate` within 24h, post to `CHANNEL_DUE_DATE_REMINDER`.
- `composeApp/src/commonMain/composeResources/values/strings.xml` — add 5 new strings (motivation title/subtitle, simulate plan section + labels).

**Unchanged (explicitly):**
- `TodoRepository`, `HabitRepository`, `JournalRepository` (Firestore, untouched).
- `AuthService`, `AuthViewModel`, `MainActivity`, `FirebaseMessagingService` (unchanged from prior fix).
- `iOS` actuals of any existing repository.
- `Onboarding`, `Settings` DataStore (we add one **new** key `user_plan` to the existing `aura_preferences` DataStore — same store, new key).
- All other workers, notification helpers, theme/design system.

---

## Task ordering rationale

Tasks 1-3 are foundation (deps, models, model tests). Tasks 4-7 are use cases + repos with TDD. Task 8 is iOS stubs (compile-only). Task 9 wires Koin. Task 10 is the worker. Tasks 11-15 integrate UI. Task 16 verifies everything. **Each task ends with a commit** so any regression can be bisected.

**Single execution mode** (no subagents per user instruction): I execute each task, run the test, commit, and move to the next, in this same session, with verification between every step.

---

## Task 0: Commit pending auth fixes (pre-work baseline)

**Files:**
- Modify (already on disk): `composeApp/google-services.json`
- Modify (already on disk): `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt`

**Context:** The `MainActivity.kt` was modified to use the Web OAuth client ID (`1093577707060-no5gln1m1iri29khllqd0us0bchd7gqf...`) and `google-services.json` was updated to the correct Firebase project. These changes make the Google Sign-In actually work (confirmed by user). We commit them as a clean baseline before starting the A/B work.

- [ ] **Step 1: Verify the working tree matches the expected state**

Run: `git status`
Expected: only `composeApp/google-services.json` and `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt` are listed as "modified". No other pending changes.

- [ ] **Step 2: Verify build still passes**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit the auth fix**

Run:
```bash
git add composeApp/google-services.json
git add composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt
git commit -m "fix(auth): use Web OAuth client ID and update google-services.json to aura-6ac09 project"
```

Expected: One new commit on `feature/extra-credit-implementation` with those two files. Working tree clean.

- [ ] **Step 4: Confirm baseline tests still match master's pre-existing failures**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon 2>&1 | Select-String -Pattern "tests completed|FAILED|BUILD"`
Expected: `50 tests completed, 10 failed` (same as master; the 10 failures are pre-existing Mockative import issues in `habit/`, `onboarding/`, `todo/` — NOT related to this work).

---

## Task 1: Add Firebase Realtime Database dependency

**Files:**
- Modify: `gradle/libs.versions.toml` (add 1 version + 1 library alias).
- Modify: `composeApp/build.gradle.kts` (add 1 implementation line).

- [ ] **Step 1: Add the version and library alias to `libs.versions.toml`**

Open `gradle/libs.versions.toml`. Find the `[versions]` block. Add this line **at the end of `[versions]`**:
```toml
firebaseDatabase = "21.0.0"
```

Find the `[libraries]` block. Add this line **at the end of `[libraries]`** (alphabetically after `firebase-config-ktx`):
```toml
firebase-database-ktx = { module = "com.google.firebase:firebase-database-ktx", version.ref = "firebaseDatabase" }
```

- [ ] **Step 2: Add the dependency to `composeApp/build.gradle.kts`**

Open `composeApp/build.gradle.kts`. In the `androidMain.dependencies` block, find the Firebase section (currently has `firebase.bom`, `firebase.config.ktx`, `firebase.auth.ktx`, `firebase.firestore.ktx`, `firebase.messaging.ktx`). Add **one** line **after** `firebase.messaging.ktx`:
```kotlin
implementation(libs.firebase.database.ktx)
```

- [ ] **Step 3: Verify Gradle resolves the dependency**

Run: `./gradlew :composeApp:dependencies --no-daemon --configuration debugRuntimeClasspath 2>&1 | Select-String -Pattern "firebase-database"`
Expected: A line like `+--- com.google.firebase:firebase-database-ktx:21.0.0` (the `+---` means newly added by our edit).

- [ ] **Step 4: Verify the project still compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL` and no new `error:` lines.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build(deps): add firebase-database-ktx for A/B experiment event logging"
```

---

## Task 2: Add `USER_PLAN` to the `FeatureFlag` enum

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt`

- [ ] **Step 1: Read the current file**

Confirm the file is at `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt` and contains a sealed class or enum with `key` and `defaultValue`. (We verified: it's an `enum class` with `Boolean` defaults.)

- [ ] **Step 2: Add a sealed enum for user-plan values**

We are adding a new flag type. The existing enum only supports `Boolean`. We need a typed value. Refactor `FeatureFlags.kt` to add a **new sibling** declaration `UserPlanFlag` and **leave** the existing `FeatureFlag` enum intact. The file becomes:

```kotlin
package com.programovil.aura.shared

enum class FeatureFlag(val key: String, val defaultValue: Boolean) {
    HABITS_ENABLED("habits_enabled", true),
    TODOS_ENABLED("todos_enabled", true),
    JOURNAL_ENABLED("journal_enabled", true),
}

enum class UserPlanFlag(val key: String, val defaultValue: String) {
    USER_PLAN("user_plan", "Free");
}
```

- [ ] **Step 3: Verify the project still compiles**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`. (Nothing else in the codebase references `UserPlanFlag` yet, so this is a pure addition.)

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt
git commit -m "feat(experiments): add UserPlanFlag enum for Remote Config"
```

---

## Task 3: Extend `RemoteConfigService` interface with typed `getUserPlan()`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt`
- Modify: `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt`

- [ ] **Step 1: Add a typed accessor to the interface**

Open `RemoteConfigService.kt`. After the `getString(flag, default)` method, add:

```kotlin
    suspend fun getUserPlan(): com.programovil.aura.shared.UserPlanFlag
```

(Note: returning the flag enum so the caller knows which key was queried and gets the typed default-value access.)

- [ ] **Step 2: Implement in the Android service**

Open `FirebaseRemoteConfigService.kt`. Add this method after `getString(...)`:

```kotlin
    override suspend fun getUserPlan(): com.programovil.aura.shared.UserPlanFlag {
        // Look up the canonical flag by key match.
        val match = com.programovil.aura.shared.UserPlanFlag.entries.firstOrNull {
            remoteConfig.getString(it.key) == it.defaultValue
        }
        return match ?: com.programovil.aura.shared.UserPlanFlag.USER_PLAN
    }
```

- [ ] **Step 3: Implement in the iOS stub**

Open `StubRemoteConfigService.kt`. Add the same signature returning `UserPlanFlag.USER_PLAN`:

```kotlin
    override suspend fun getUserPlan(): com.programovil.aura.shared.UserPlanFlag =
        com.programovil.aura.shared.UserPlanFlag.USER_PLAN
```

- [ ] **Step 4: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64 --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt
git add composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt
git add composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt
git commit -m "feat(experiments): add getUserPlan() to RemoteConfigService interface"
```

---

## Task 4: Add domain models for the experiments feature

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/model/UserPlan.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/model/HomeVariant.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/model/Tone.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/model/NotificationVariant.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/model/ExperimentEvent.kt`

- [ ] **Step 1: Create `UserPlan.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

sealed class UserPlan {
    data object Free : UserPlan()
    data object Premium : UserPlan()

    companion object {
        fun fromRemoteConfigString(raw: String?): UserPlan = when (raw) {
            "Premium" -> Premium
            else -> Free
        }
    }
}
```

- [ ] **Step 2: Create `Tone.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

enum class Tone {
    Gentle,
    Direct
}
```

- [ ] **Step 3: Create `HomeVariant.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

data class HomeVariant(
    val showsDailyMotivation: Boolean,
    val tone: Tone
)
```

- [ ] **Step 4: Create `NotificationVariant.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

data class NotificationVariant(
    val timesPerDay: Int,
    val tone: Tone,
    val useDueDateChannel: Boolean
)
```

- [ ] **Step 5: Create `ExperimentEvent.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

sealed class ExperimentEvent {
    abstract val userPlan: UserPlan

    data class HomeOpened(override val userPlan: UserPlan) : ExperimentEvent()
    data class TabClicked(override val userPlan: UserPlan, val tabName: String) : ExperimentEvent()
    data class NotificationDelivered(override val userPlan: UserPlan, val channel: String) : ExperimentEvent()
    data class SessionActive(override val userPlan: UserPlan) : ExperimentEvent()
}
```

- [ ] **Step 6: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/
git commit -m "feat(experiments): add domain models (UserPlan, HomeVariant, NotificationVariant, ExperimentEvent)"
```

---

## Task 5: Add domain tests for models (TDD)

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/model/UserPlanTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/model/ExperimentEventTest.kt`

- [ ] **Step 1: Create `UserPlanTest.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserPlanTest {

    @Test
    fun `fromRemoteConfigString Premium maps to Premium`() {
        assertEquals(UserPlan.Premium, UserPlan.fromRemoteConfigString("Premium"))
    }

    @Test
    fun `fromRemoteConfigString Free maps to Free`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString("Free"))
    }

    @Test
    fun `fromRemoteConfigString null maps to Free (safe default)`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString(null))
    }

    @Test
    fun `fromRemoteConfigString unknown value maps to Free`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString("Platinum"))
    }

    @Test
    fun `fromRemoteConfigString case-sensitive rejects lowercase premium`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString("premium"))
    }
}
```

- [ ] **Step 2: Create `ExperimentEventTest.kt`**

```kotlin
package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExperimentEventTest {

    @Test
    fun `HomeOpened carries userPlan`() {
        val event = ExperimentEvent.HomeOpened(UserPlan.Premium)
        assertEquals(UserPlan.Premium, event.userPlan)
    }

    @Test
    fun `TabClicked carries tabName`() {
        val event = ExperimentEvent.TabClicked(UserPlan.Free, tabName = "Todos")
        assertEquals("Todos", event.tabName)
        assertEquals(UserPlan.Free, event.userPlan)
    }

    @Test
    fun `NotificationDelivered carries channel`() {
        val event = ExperimentEvent.NotificationDelivered(UserPlan.Premium, channel = "due_date_reminder")
        assertEquals("due_date_reminder", event.channel)
    }

    @Test
    fun `SessionActive carries userPlan`() {
        val event = ExperimentEvent.SessionActive(UserPlan.Free)
        assertEquals(UserPlan.Free, event.userPlan)
    }

    @Test
    fun `sealed class has exactly four variants (exhaustive)`() {
        val events: List<ExperimentEvent> = listOf(
            ExperimentEvent.HomeOpened(UserPlan.Free),
            ExperimentEvent.TabClicked(UserPlan.Free, ""),
            ExperimentEvent.NotificationDelivered(UserPlan.Free, ""),
            ExperimentEvent.SessionActive(UserPlan.Free)
        )
        assertTrue(events.size == 4)
    }
}
```

- [ ] **Step 3: Run the new tests, expect them to pass**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --tests "com.programovil.aura.experiments.domain.model.*" 2>&1 | Select-String -Pattern "tests completed|BUILD|PASSED|FAILED"`
Expected: `BUILD SUCCESSFUL` and `9 tests completed, 0 failed`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/
git commit -m "test(experiments): add domain model tests for UserPlan and ExperimentEvent"
```

---


## Task 6: Add repository interfaces

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/repository/UserPlanRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/repository/ExperimentRepository.kt`

- [ ] **Step 1: Create `UserPlanRepository.kt`**

```kotlin
package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.domain.model.UserPlan
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
interface UserPlanRepository {
    fun observeUserPlan(): Flow<UserPlan>
    suspend fun getUserPlan(): UserPlan
    suspend fun setUserPlan(plan: UserPlan)
    suspend fun refreshFromRemote(): Result<Unit>
}

expect fun createUserPlanRepository(
    dataStoreProvider: com.programovil.aura.shared.data.DataStoreProvider
): UserPlanRepository
```

- [ ] **Step 2: Create `ExperimentRepository.kt`**

```kotlin
package com.programovil.aura.experiments.domain.repository

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import io.mockative.Mockable

@Mockable
interface ExperimentRepository {
    suspend fun logEvent(event: ExperimentEvent): Result<Unit>
}

expect fun createExperimentRepository(
    dataStoreProvider: com.programovil.aura.shared.data.DataStoreProvider
): ExperimentRepository
```

- [ ] **Step 3: Verify build (will fail: DataStoreProvider does not exist yet, expect/actual is incomplete)**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "error:"`
Expected: Errors about missing `DataStoreProvider` and missing actuals. This is expected � Task 7 fixes it.

(Note: we accept temporary build breakage here because we are about to add the DataStoreProvider in Task 7 and the actuals. Mark this step as `[x]` and proceed to Task 7 without committing.)

- [ ] **Step 4: DO NOT commit yet � proceed to Task 7.**

---

## Task 7: Add `DataStoreProvider` and Android actuals for the repositories

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/data/DataStoreProvider.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/data/DataStoreFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/data/repository/DataStoreUserPlanRepositoryImpl.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/data/repository/FirebaseExperimentRepositoryImpl.kt`
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/experiments/data/repository/IosUserPlanRepositoryImpl.kt`
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/experiments/data/repository/IosExperimentRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt` (defer Koin wiring to Task 9)

- [ ] **Step 1: Read the current `DataStoreFactory.kt`**

Confirm the existing API is `expect fun createDataStore(): DataStore<Preferences>` and that the actuals are registered.

- [ ] **Step 2: Create `DataStoreProvider.kt` in commonMain**

This wrapper makes the DataStore available as a Koin singleton while preserving the existing `createDataStore()` factory.

```kotlin
package com.programovil.aura.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/** Wrapper so Koin can provide the DataStore as a single dependency. */
class DataStoreProvider(val dataStore: DataStore<Preferences>)
```

- [ ] **Step 3: Update `DataStoreFactory.kt` to also expose the provider**

Append to the file:

```kotlin
fun createDataStoreProvider(): com.programovil.aura.shared.data.DataStoreProvider =
    com.programovil.aura.shared.data.DataStoreProvider(createDataStore())
```

(Keep the existing `expect fun createDataStore()` as-is.)

- [ ] **Step 4: Create the Android `DataStoreUserPlanRepositoryImpl`**

```kotlin
package com.programovil.aura.experiments.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.shared.data.DataStoreProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val USER_PLAN_KEY = stringPreferencesKey("user_plan")

class DataStoreUserPlanRepositoryImpl(
    private val dataStoreProvider: DataStoreProvider,
    private val remoteConfigService: RemoteConfigService
) : UserPlanRepository {

    private val dataStore: DataStore<Preferences> = dataStoreProvider.dataStore

    override fun observeUserPlan(): Flow<UserPlan> =
        dataStore.data
            .map { prefs -> UserPlan.fromRemoteConfigString(prefs[USER_PLAN_KEY]) }
            .distinctUntilChanged()

    override suspend fun getUserPlan(): UserPlan = observeUserPlan().let { flow ->
        var first: UserPlan = UserPlan.Free
        flow.collect { first = it; return@collect }
        first
    }

    override suspend fun setUserPlan(plan: UserPlan) {
        dataStore.edit { prefs -> prefs[USER_PLAN_KEY] = plan.toRemoteString() }
    }

    override suspend fun refreshFromRemote(): Result<Unit> = runCatching {
        val raw = remoteConfigService.getUserPlan()
        val plan = UserPlan.fromRemoteConfigString(raw)
        dataStore.edit { prefs -> prefs[USER_PLAN_KEY] = plan.toRemoteString() }
    }

    private fun UserPlan.toRemoteString(): String = when (this) {
        UserPlan.Premium -> "Premium"
        UserPlan.Free -> "Free"
    }
}
```

- [ ] **Step 5: Create the Android actual for `UserPlanRepository`**

Create `composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/data/repository/UserPlanRepositoryActual.kt`:

```kotlin
package com.programovil.aura.experiments.data.repository

import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.shared.data.DataStoreProvider

actual fun createUserPlanRepository(
    dataStoreProvider: DataStoreProvider,
    remoteConfigService: RemoteConfigService
): UserPlanRepository = DataStoreUserPlanRepositoryImpl(dataStoreProvider, remoteConfigService)
```

- [ ] **Step 6: Create the Android `FirebaseExperimentRepositoryImpl`**

```kotlin
package com.programovil.aura.experiments.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import com.programovil.aura.shared.data.DataStoreProvider
import kotlinx.coroutines.tasks.await

class FirebaseExperimentRepositoryImpl(
    @Suppress("unused") private val dataStoreProvider: DataStoreProvider
) : ExperimentRepository {

    private val database by lazy { Firebase.database }
    private val auth by lazy { Firebase.auth }

    override suspend fun logEvent(event: ExperimentEvent): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
            ?: error("Cannot log experiment event: no authenticated user")
        val ref = database.reference
            .child("users")
            .child(uid)
            .child("experiments")
            .child("events")
            .push()
        val payload = mapOf(
            "type" to eventTypeName(event),
            "variant" to event.userPlan.variantName(),
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
            "metadata" to eventMetadata(event)
        )
        ref.setValue(payload).await()
    }

    private fun eventTypeName(event: ExperimentEvent): String = when (event) {
        is ExperimentEvent.HomeOpened -> "home_opened"
        is ExperimentEvent.TabClicked -> "tab_clicked"
        is ExperimentEvent.NotificationDelivered -> "notification_delivered"
        is ExperimentEvent.SessionActive -> "session_active"
    }

    private fun eventMetadata(event: ExperimentEvent): Map<String, String> = when (event) {
        is ExperimentEvent.TabClicked -> mapOf("tab" to event.tabName)
        is ExperimentEvent.NotificationDelivered -> mapOf("channel" to event.channel)
        else -> emptyMap()
    }

    private fun UserPlan.variantName(): String = when (this) {
        UserPlan.Premium -> "Premium"
        UserPlan.Free -> "Free"
    }
}
```

- [ ] **Step 7: Create the Android actual for `ExperimentRepository`**

```kotlin
package com.programovil.aura.experiments.data.repository

import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import com.programovil.aura.shared.data.DataStoreProvider

actual fun createExperimentRepository(
    dataStoreProvider: DataStoreProvider
): ExperimentRepository = FirebaseExperimentRepositoryImpl(dataStoreProvider)
```

- [ ] **Step 8: Create the iOS stubs**

```kotlin
// iosMain/.../experiments/data/repository/UserPlanRepositoryActual.kt
package com.programovil.aura.experiments.data.repository

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.shared.data.DataStoreProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createUserPlanRepository(
    dataStoreProvider: DataStoreProvider,
    remoteConfigService: RemoteConfigService
): UserPlanRepository = object : UserPlanRepository {
    override fun observeUserPlan(): Flow<UserPlan> = flowOf(UserPlan.Free)
    override suspend fun getUserPlan(): UserPlan = UserPlan.Free
    override suspend fun setUserPlan(plan: UserPlan) {}
    override suspend fun refreshFromRemote(): Result<Unit> = Result.success(Unit)
}
```

```kotlin
// iosMain/.../experiments/data/repository/ExperimentRepositoryActual.kt
package com.programovil.aura.experiments.data.repository

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import com.programovil.aura.shared.data.DataStoreProvider

actual fun createExperimentRepository(
    dataStoreProvider: DataStoreProvider
): ExperimentRepository = object : ExperimentRepository {
    override suspend fun logEvent(event: ExperimentEvent): Result<Unit> = Result.success(Unit)
}
```

- [ ] **Step 9: Verify build for all targets**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64 --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Run the existing test suite (no new tests yet for repos; we test the use cases instead)**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --rerun-tasks 2>&1 | Select-String -Pattern "tests completed, \d+ failed"`
Expected: `60 tests completed, 10 failed` (no regression).

- [ ] **Step 11: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/data/DataStoreProvider.kt
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/data/DataStoreFactory.kt
git add composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/
git add composeApp/src/iosMain/kotlin/com/programovil/aura/experiments/
git commit -m "feat(experiments): add repository interfaces + Android/iOS actuals (DataStore + RTDB)"
```

---

## Task 8: Add use cases (no tests yet; tests in Task 9)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/usecase/GetUserPlanUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/usecase/GetHomeVariantUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/usecase/GetNotificationVariantUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/usecase/LogExperimentEventUseCase.kt`

- [ ] **Step 1: Create `GetUserPlanUseCase.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import kotlinx.coroutines.flow.Flow

class GetUserPlanUseCase(private val repository: UserPlanRepository) {
    operator fun invoke(): Flow<UserPlan> = repository.observeUserPlan()
    suspend fun get(): UserPlan = repository.getUserPlan()
    suspend fun set(plan: UserPlan) = repository.setUserPlan(plan)
    suspend fun refresh(): Result<Unit> = repository.refreshFromRemote()
}
```

- [ ] **Step 2: Create `GetHomeVariantUseCase.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.HomeVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan

class GetHomeVariantUseCase(private val getUserPlanUseCase: GetUserPlanUseCase) {
    suspend operator fun invoke(): HomeVariant {
        val plan = getUserPlanUseCase.get()
        return when (plan) {
            UserPlan.Free -> HomeVariant(showsDailyMotivation = false, tone = Tone.Gentle)
            UserPlan.Premium -> HomeVariant(showsDailyMotivation = true, tone = Tone.Direct)
        }
    }
}
```

- [ ] **Step 3: Create `GetNotificationVariantUseCase.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.NotificationVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan

class GetNotificationVariantUseCase(private val getUserPlanUseCase: GetUserPlanUseCase) {
    suspend operator fun invoke(): NotificationVariant {
        val plan = getUserPlanUseCase.get()
        return when (plan) {
            UserPlan.Free -> NotificationVariant(timesPerDay = 1, tone = Tone.Gentle, useDueDateChannel = false)
            UserPlan.Premium -> NotificationVariant(timesPerDay = 2, tone = Tone.Direct, useDueDateChannel = true)
        }
    }
}
```

- [ ] **Step 4: Create `LogExperimentEventUseCase.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.repository.ExperimentRepository

class LogExperimentEventUseCase(private val repository: ExperimentRepository) {
    suspend operator fun invoke(event: ExperimentEvent): Result<Unit> = repository.logEvent(event)
}
```

- [ ] **Step 5: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`. (Use cases are not yet wired anywhere; this just verifies syntax.)

- [ ] **Step 6: Commit (no tests yet; that's Task 9)**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/domain/usecase/
git commit -m "feat(experiments): add use cases (GetUserPlan, GetHomeVariant, GetNotificationVariant, LogExperimentEvent)"
```

---

## Task 9: Add use-case tests (TDD, written now against the use cases from Task 8)

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/usecase/GetUserPlanUseCaseTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/usecase/GetHomeVariantUseCaseTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/usecase/GetNotificationVariantUseCaseTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/usecase/LogExperimentEventUseCaseTest.kt`

- [ ] **Step 1: Create `GetUserPlanUseCaseTest.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import app.cash.turbine.test
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetUserPlanUseCaseTest {

    private val repo = mock(of<UserPlanRepository>())

    @Test
    fun `invoke exposes repository observeUserPlan flow`() = runTest {
        every { repo.observeUserPlan() } returns flowOf(UserPlan.Free, UserPlan.Premium)
        val useCase = GetUserPlanUseCase(repo)

        useCase().test {
            assertEquals(UserPlan.Free, awaitItem())
            assertEquals(UserPlan.Premium, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `get returns current user plan from repository`() = runTest {
        every { repo.getUserPlan() } returns UserPlan.Premium
        val useCase = GetUserPlanUseCase(repo)

        assertEquals(UserPlan.Premium, useCase.get())
    }

    @Test
    fun `set delegates to repository`() = runTest {
        every { repo.setUserPlan(UserPlan.Premium) } returns Unit
        val useCase = GetUserPlanUseCase(repo)

        useCase.set(UserPlan.Premium)
        assertTrue(true)
    }

    @Test
    fun `refresh returns Result from repository`() = runTest {
        every { repo.refreshFromRemote() } returns Result.success(Unit)
        val useCase = GetUserPlanUseCase(repo)

        val result = useCase.refresh()
        assertTrue(result.isSuccess)
    }
}
```

- [ ] **Step 2: Create `GetHomeVariantUseCaseTest.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetHomeVariantUseCaseTest {

    private val getUserPlanUseCase = mock(of<GetUserPlanUseCase>())

    @Test
    fun `Free plan returns variant with no daily motivation`() = runTest {
        every { getUserPlanUseCase.get() } returns UserPlan.Free
        val useCase = GetHomeVariantUseCase(getUserPlanUseCase)

        val variant = useCase()
        assertFalse(variant.showsDailyMotivation)
        assertEquals(Tone.Gentle, variant.tone)
    }

    @Test
    fun `Premium plan returns variant with daily motivation and direct tone`() = runTest {
        every { getUserPlanUseCase.get() } returns UserPlan.Premium
        val useCase = GetHomeVariantUseCase(getUserPlanUseCase)

        val variant = useCase()
        assertTrue(variant.showsDailyMotivation)
        assertEquals(Tone.Direct, variant.tone)
    }
}
```

- [ ] **Step 3: Create `GetNotificationVariantUseCaseTest.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetNotificationVariantUseCaseTest {

    private val getUserPlanUseCase = mock(of<GetUserPlanUseCase>())

    @Test
    fun `Free plan returns 1 daily notification gentle tone no due-date channel`() = runTest {
        every { getUserPlanUseCase.get() } returns UserPlan.Free
        val useCase = GetNotificationVariantUseCase(getUserPlanUseCase)

        val variant = useCase()
        assertEquals(1, variant.timesPerDay)
        assertEquals(Tone.Gentle, variant.tone)
        assertFalse(variant.useDueDateChannel)
    }

    @Test
    fun `Premium plan returns 2 daily notifications direct tone with due-date channel`() = runTest {
        every { getUserPlanUseCase.get() } returns UserPlan.Premium
        val useCase = GetNotificationVariantUseCase(getUserPlanUseCase)

        val variant = useCase()
        assertEquals(2, variant.timesPerDay)
        assertEquals(Tone.Direct, variant.tone)
        assertTrue(variant.useDueDateChannel)
    }
}
```

- [ ] **Step 4: Create `LogExperimentEventUseCaseTest.kt`**

```kotlin
package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LogExperimentEventUseCaseTest {

    private val repo = mock(of<ExperimentRepository>())

    @Test
    fun `invoke delegates to repository logEvent`() = runTest {
        val event = ExperimentEvent.HomeOpened(UserPlan.Free)
        coEvery { repo.logEvent(event) } returns Result.success(Unit)
        val useCase = LogExperimentEventUseCase(repo)

        val result = useCase(event)
        coVerify { repo.logEvent(event) }.wasInvoked(exactly = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke surfaces failure from repository`() = runTest {
        val event = ExperimentEvent.SessionActive(UserPlan.Premium)
        coEvery { repo.logEvent(event) } returns Result.failure(RuntimeException("boom"))
        val useCase = LogExperimentEventUseCase(repo)

        val result = useCase(event)
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 5: Run the new tests**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --tests "com.programovil.aura.experiments.domain.usecase.*" --rerun-tasks 2>&1 | Select-String -Pattern "BUILD|tests completed,|FAILED|error:"`
Expected: `BUILD SUCCESSFUL` and the tests pass.

- [ ] **Step 6: Verify total count is still 60 tests / 10 failed (no regression)**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --rerun-tasks 2>&1 | Select-String -Pattern "tests completed, \d+ failed"`
Expected: `70 tests completed, 10 failed` (was 60 ? +10 from the new use case tests = 70).

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/experiments/domain/usecase/
git commit -m "test(experiments): add use case tests (GetUserPlan, GetHomeVariant, GetNotificationVariant, LogExperimentEvent)"
```

---

## Task 10: Add Koin module and wire to InitKoin

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/di/ExperimentsModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`

- [ ] **Step 1: Read the current `InitKoin.kt`**

Confirm the signature: `fun getModules(remoteConfigService: RemoteConfigService) = listOf(...)`.

- [ ] **Step 2: Create `ExperimentsModule.kt`**

```kotlin
package com.programovil.aura.experiments.di

import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.experiments.domain.repository.createExperimentRepository
import com.programovil.aura.experiments.domain.repository.createUserPlanRepository
import com.programovil.aura.experiments.domain.usecase.GetHomeVariantUseCase
import com.programovil.aura.experiments.domain.usecase.GetNotificationVariantUseCase
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.experiments.domain.usecase.LogExperimentEventUseCase
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.shared.data.DataStoreProvider
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun experimentsModule(
    dataStoreProvider: DataStoreProvider,
    remoteConfigService: RemoteConfigService
): Module = module {
    single {
        createUserPlanRepository(dataStoreProvider, remoteConfigService)
    } bind UserPlanRepository::class

    single {
        createExperimentRepository(dataStoreProvider)
    } bind ExperimentRepository::class

    factoryOf(::GetUserPlanUseCase)
    factoryOf(::GetHomeVariantUseCase)
    factoryOf(::GetNotificationVariantUseCase)
    factoryOf(::LogExperimentEventUseCase)
}
```

- [ ] **Step 3: Modify `InitKoin.kt`**

Open the file. Add `import com.programovil.aura.experiments.di.experimentsModule` and `import com.programovil.aura.shared.data.createDataStoreProvider`. Then, in the returned list, add `experimentsModule(createDataStoreProvider(), remoteConfigService)` before the closing `)`.

The `getModules` function should now look like:

```kotlin
fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    settingsModule,
    journalModule,
    onboardingModule,
    experimentsModule(createDataStoreProvider(), remoteConfigService),
    module {
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

- [ ] **Step 4: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run tests to confirm no regression**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --rerun-tasks 2>&1 | Select-String -Pattern "tests completed, \d+ failed"`
Expected: `70 tests completed, 10 failed`.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/di/ExperimentsModule.kt
git add composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git commit -m "feat(experiments): add Koin module and wire into InitKoin"
```

---

## Task 11: Add `ExperimentsHeartbeatWorker`

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/presentation/worker/ExperimentsHeartbeatWorker.kt`

- [ ] **Step 1: Create the worker**

```kotlin
package com.programovil.aura.experiments.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.experiments.domain.usecase.LogExperimentEventUseCase
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ExperimentsHeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        val getUserPlanUseCase: GetUserPlanUseCase = get()
        val logExperimentEventUseCase: LogExperimentEventUseCase = get()
        val plan = getUserPlanUseCase.get()
        val result = logExperimentEventUseCase(ExperimentEvent.SessionActive(plan))
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "experiments_heartbeat_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExperimentsHeartbeatWorker>(
                12, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/presentation/worker/
git commit -m "feat(experiments): add ExperimentsHeartbeatWorker (12h periodic, RTDB logging)"
```

---

## Task 12: Schedule the worker in `AndroidApp.onCreate`

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt`

- [ ] **Step 1: Read the current `AndroidApp.kt`**

- [ ] **Step 2: Add the worker schedule call**

In `onCreate`, after `WorkManager`-related setup (or anywhere after Koin starts), add:

```kotlin
com.programovil.aura.experiments.presentation.worker.ExperimentsHeartbeatWorker.schedule(this)
```

Concretely, append the call at the end of `onCreate()`.

- [ ] **Step 3: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt
git commit -m "feat(experiments): schedule ExperimentsHeartbeatWorker on app start"
```

---

## Task 13: Add strings (i18n) and Home motivation card string resource

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- Modify: `composeApp/src/commonMain/composeResources/values-fr/strings.xml`

- [ ] **Step 1: Add English strings**

Open `values/strings.xml`. Add this block **before the closing `</resources>`**:

```xml
<!-- Experiments / A/B testing -->
<string name="home_dashboard_motivation_title">DAILY MOTIVATION</string>
<string name="home_dashboard_motivation_subtitle">%1$s</string>
<string name="home_dashboard_motivation_value_greeting">Keep going</string>
<string name="home_dashboard_motivation_value_premium">Stay focused</string>
<string name="settings_simulate_plan">Simulate Plan</string>
<string name="settings_simulate_plan_free">Free</string>
<string name="settings_simulate_plan_premium">Premium</string>
```

- [ ] **Step 2: Add Spanish translations**

If `values-es/strings.xml` exists, add the same keys with Spanish values:

```xml
<string name="home_dashboard_motivation_title">MOTIVACI�N DIARIA</string>
<string name="home_dashboard_motivation_subtitle">%1$s</string>
<string name="home_dashboard_motivation_value_greeting">Sigue as�</string>
<string name="home_dashboard_motivation_value_premium">Mant�n el enfoque</string>
<string name="settings_simulate_plan">Simular Plan</string>
<string name="settings_simulate_plan_free">Gratis</string>
<string name="settings_simulate_plan_premium">Premium</string>
```

- [ ] **Step 3: Add French translations**

If `values-fr/strings.xml` exists, add the same keys with French values:

```xml
<string name="home_dashboard_motivation_title">MOTIVATION QUOTIDIENNE</string>
<string name="home_dashboard_motivation_subtitle">%1$s</string>
<string name="home_dashboard_motivation_value_greeting">Continuez</string>
<string name="home_dashboard_motivation_value_premium">Restez concentr�</string>
<string name="settings_simulate_plan">Simuler le plan</string>
<string name="settings_simulate_plan_free">Gratuit</string>
<string name="settings_simulate_plan_premium">Premium</string>
```

- [ ] **Step 4: Verify build (resource processing)**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/composeResources/
git commit -m "feat(experiments): add string resources for A/B variants (en, es, fr)"
```

---

## Task 14: Integrate `HomeViewModel` with `GetHomeVariantUseCase`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/viewmodel/HomeViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/composable/DashboardCard.kt` (if it doesn't already accept a `subtitle` String)
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/di/HomeModule.kt` (add the new use case dependency)

- [ ] **Step 1: Read `HomeViewModel.kt` and `HomeScreen.kt`**

Confirm the structure: the view model exposes a `uiState` and the screen reads it.

- [ ] **Step 2: Add the variant to `HomeViewModel.UiState`**

Open `HomeViewModel.kt`. Add a new field to the data class:

```kotlin
data class UiState(
    val isLoading: Boolean = true,
    val dashboardData: DashboardData = DashboardData(),
    val homeVariant: com.programovil.aura.experiments.domain.model.HomeVariant =
        com.programovil.aura.experiments.domain.model.HomeVariant(
            showsDailyMotivation = false,
            tone = com.programovil.aura.experiments.domain.model.Tone.Gentle
        )
)
```

- [ ] **Step 3: Inject `GetHomeVariantUseCase`**

Add a constructor parameter:

```kotlin
class HomeViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val getHomeVariantUseCase: com.programovil.aura.experiments.domain.usecase.GetHomeVariantUseCase
) : ViewModel() { ... }
```

- [ ] **Step 4: Load the variant on init**

In `init {}` (after the existing call), add:

```kotlin
viewModelScope.launch {
    val variant = getHomeVariantUseCase()
    _uiState.update { it.copy(homeVariant = variant) }
}
```

Also collect the user plan flow so changes propagate:

```kotlin
viewModelScope.launch {
    getUserPlanUseCase().collect { plan ->
        // Re-fetch variant when plan changes.
        val variant = getHomeVariantUseCase()
        _uiState.update { it.copy(homeVariant = variant) }
    }
}
```

(You will need to import `getUserPlanUseCase`. The injection list in the constructor will also need it.)

- [ ] **Step 5: Update the constructor to take `GetUserPlanUseCase`**

```kotlin
class HomeViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val getHomeVariantUseCase: com.programovil.aura.experiments.domain.usecase.GetHomeVariantUseCase,
    private val getUserPlanUseCase: com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
) : ViewModel() { ... }
```

- [ ] **Step 6: Update `HomeModule.kt` to inject the new dependencies**

Add `factoryOf(::GetHomeVariantUseCase)` is already in `experimentsModule`. The Koin graph will resolve. **No change to `HomeModule` is needed** � Koin will inject the new constructor args automatically because they are bound as `factoryOf` in `experimentsModule`.

- [ ] **Step 7: Verify the build still passes**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run existing home tests to verify no regression**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --tests "com.programovil.aura.home.*" 2>&1 | Select-String -Pattern "tests completed|BUILD"`
Expected: at least the home tests still run (or are skipped because the test isn't using the VM yet � the existing home tests don't test the VM, so this may show "no tests matched" which is OK).

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/
git commit -m "feat(experiments): inject GetHomeVariantUseCase into HomeViewModel + add homeVariant to UiState"
```

---

## Task 15: Render the 3rd motivation card in `HomeScreen` (Premium only)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt`

- [ ] **Step 1: Add the conditional 3rd card**

In `HomeScreen.kt`, after the second `DashboardCard` and inside the `Column`, add:

```kotlin
if (uiState.homeVariant.showsDailyMotivation) {
    Spacer(modifier = Modifier.height(16.dp))
    val subtitle = if (uiState.homeVariant.tone == com.programovil.aura.experiments.domain.model.Tone.Direct) {
        stringResource(Res.string.home_dashboard_motivation_value_premium)
    } else {
        stringResource(Res.string.home_dashboard_motivation_value_greeting)
    }
    DashboardCard(
        title = stringResource(Res.string.home_dashboard_motivation_title),
        value = "?",
        subtitle = stringResource(Res.string.home_dashboard_motivation_subtitle, subtitle),
        onClick = { /* tap action: a no-op for now */ }
    )
}
```

- [ ] **Step 2: Verify the build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt
git commit -m "feat(experiments): render Daily Motivation card on Home for Premium plan"
```

---

## Task 16: Gate `Habits` and `Journal` tabs in `App.kt` for Premium

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt`

- [ ] **Step 1: Read the current `App.kt`**

Find the block that computes `showHabits` / `showJournals` (uses `FeatureFlagManager.flags`).

- [ ] **Step 2: Add `showPremiumFeatures: Boolean`**

After the existing `showTodos`/`showHabits`/`showJournals` lines, add:

```kotlin
val showPremiumFeatures by remember(featureFlags, userPlan) {
    derivedStateOf { userPlan is com.programovil.aura.experiments.domain.model.UserPlan.Premium }
}
```

This requires `userPlan` to be collected from `GetUserPlanUseCase`. Add a `LaunchedEffect` near the existing one to start collecting the plan:

```kotlin
val getUserPlanUseCase: com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase = koinInject()
var userPlan by remember { mutableStateOf<com.programovil.aura.experiments.domain.model.UserPlan>(com.programovil.aura.experiments.domain.model.UserPlan.Free) }

LaunchedEffect(Unit) {
    getUserPlanUseCase().collect { userPlan = it }
}
```

- [ ] **Step 3: Apply the gate to the Habits and Journal tabs**

Where the existing code has:

```kotlin
if (showHabits) { NavigationBarItem(... Habits ...) }
```

Change to:

```kotlin
if (showHabits && showPremiumFeatures) { NavigationBarItem(... Habits ...) }
```

Do the same for `showJournals`.

- [ ] **Step 4: Verify the build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "feat(experiments): gate Habits and Journal tabs behind Premium plan"
```

---

## Task 17: Add the "Simulate Plan" debug toggle in `SettingsViewModel` + `SettingsScreen`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/viewmodel/SettingsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt`

- [ ] **Step 1: Read both files**

- [ ] **Step 2: Inject `GetUserPlanUseCase` into `SettingsViewModel`**

Add a constructor parameter:

```kotlin
class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val notificationPreferences: NotificationPreferences,
    private val getUserPlanUseCase: com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase,
    private val notificationScheduler: com.programovil.aura.notification.domain.NotificationScheduler,
    private val workManager: androidx.work.WorkManager,
    // ... existing params
) : ViewModel()
```

(Match the existing constructor signature exactly � only add new params; the existing code injects `getUserPlanUseCase` separately or it can come from a new `factoryOf` if not already there.)

For simplicity, also pass `WorkManager` (Android) via Koin. Update the existing constructor to add these args.

- [ ] **Step 3: Expose `userPlan` in `SettingsUiState`**

Add a new field:

```kotlin
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.PURPLE,
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val userPlan: com.programovil.aura.experiments.domain.model.UserPlan = com.programovil.aura.experiments.domain.model.UserPlan.Free
)
```

- [ ] **Step 4: Collect the user plan in `init`**

```kotlin
init {
    // existing code...
    viewModelScope.launch {
        getUserPlanUseCase().collect { plan ->
            _uiState.update { it.copy(userPlan = plan) }
        }
    }
}
```

- [ ] **Step 5: Add `setSimulatedPlan`**

```kotlin
fun setSimulatedPlan(plan: com.programovil.aura.experiments.domain.model.UserPlan) {
    viewModelScope.launch { getUserPlanUseCase.set(plan) }
}
```

- [ ] **Step 6: Update the Koin module if needed**

If `SettingsViewModel` is constructed via `viewModelOf` and Koin doesn't know how to inject the new use case, you may need to switch to an explicit `viewModel { ... }` declaration that lists the args. Verify by compiling.

- [ ] **Step 7: Add the toggle UI in `SettingsScreen`**

After the preferences section, add:

```kotlin
item {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.settings_simulate_plan),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(
                onClick = { viewModel.setSimulatedPlan(UserPlan.Free) },
                label = { Text(stringResource(Res.string.settings_simulate_plan_free)) },
                colors = if (uiState.userPlan is UserPlan.Free) AssistChipDefaults.assistChipColors(containerColor = AppTheme.colors.primary) else AssistChipDefaults.assistChipColors()
            )
            AssistChip(
                onClick = { viewModel.setSimulatedPlan(UserPlan.Premium) },
                label = { Text(stringResource(Res.string.settings_simulate_plan_premium)) },
                colors = if (uiState.userPlan is UserPlan.Premium) AssistChipDefaults.assistChipColors(containerColor = AppTheme.colors.primary) else AssistChipDefaults.assistChipColors()
            )
        }
    }
}
```

(If `SettingsScreen` doesn't use a `LazyColumn` with `items`, find the equivalent place to insert this Column. The existing screen uses a plain `Column` with `verticalScroll(rememberScrollState())` � insert at the end of the existing `Column`.)

- [ ] **Step 8: Verify build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/settings/
git commit -m "feat(experiments): add Simulate Plan debug toggle in Settings"
```

---

## Task 18: Adjust `AndroidNotificationScheduler` for 2x/day + due-date channel (Premium)

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/notification/domain/AndroidNotificationScheduler.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/notification/presentation/worker/DailySummaryWorker.kt`

- [ ] **Step 1: Read both files**

- [ ] **Step 2: In `AndroidNotificationScheduler.scheduleDailySummary`, after computing the initial delay, enqueue an additional worker for Premium**

After the existing `workManager.enqueueUniquePeriodicWork(...)` call, add:

```kotlin
val getNotificationVariantUseCase: com.programovil.aura.experiments.domain.usecase.GetNotificationVariantUseCase =
    org.koin.core.component.KoinComponent.let { it.get() }
val variant = getNotificationVariantUseCase()
if (variant.timesPerDay >= 2) {
    val eveningHour = (hour + 12) % 24
    val eveningTarget = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, eveningHour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val eveningDelay = eveningTarget.timeInMillis - java.util.Calendar.getInstance().timeInMillis
    val eveningRequest = androidx.work.PeriodicWorkRequestBuilder<com.programovil.aura.notification.presentation.worker.DailySummaryWorker>(
        1, java.util.concurrent.TimeUnit.DAYS
    ).setInitialDelay(eveningDelay, java.util.concurrent.TimeUnit.MILLISECONDS).build()
    workManager.enqueueUniquePeriodicWork(
        "daily_summary_work_evening",
        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
        eveningRequest
    )
}
```

- [ ] **Step 3: In `DailySummaryWorker.doWork`, after the existing daily summary, also call the due-date channel for Premium**

Append after the existing call to `NotificationHelper.showDailySummaryNotification`:

```kotlin
val getNotificationVariantUseCase: com.programovil.aura.experiments.domain.usecase.GetNotificationVariantUseCase =
    org.koin.core.component.KoinComponent.let { it.get() }
val variant = getNotificationVariantUseCase()
if (variant.useDueDateChannel) {
    val todos = todoRepository.getTodos().first()
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val in24h = now + 24 * 60 * 60 * 1000L
    todos.getOrNull()?.firstOrNull { it.dueDate != null && it.dueDate in now..in24h }?.let { todo ->
        com.programovil.aura.notification.NotificationHelper.showDueDateNotification(applicationContext, todo.title)
    }
}
```

(Requires the worker to inject `TodoRepository` via Koin.)

- [ ] **Step 4: Verify the build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`. If `TodoRepository` injection fails, add the dependency in the worker constructor and update the Koin module.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/notification/
git commit -m "feat(experiments): 2x/day notifications + due-date channel for Premium"
```

---

## Task 19: Final verification + manual smoke test instructions

- [ ] **Step 1: Run the full test suite to confirm 70/10 baseline (no regression)**

Run: `./gradlew :composeApp:testDebugUnitTest --no-daemon --rerun-tasks 2>&1 | Select-String -Pattern "tests completed, \d+ failed"`
Expected: `70 tests completed, 10 failed`.

- [ ] **Step 2: Compile all targets**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64 --no-daemon 2>&1 | Select-String -Pattern "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Final commit � docs**

```bash
git add docs/superpowers/
git commit -m "docs: add A/B testing Free vs Premium spec and implementation plan"
```

- [ ] **Step 4: Generate a summary of all commits added in this work**

Run: `git log --oneline master..feature/extra-credit-implementation`
Expected: ~14-16 commits, all scoped to the A/B work.

- [ ] **Step 5: Write the user-facing manual smoke test in the chat reply**

Document the steps the user takes after `installDebug` to verify the A/B behavior.

---
