# A/B Testing: Free vs Premium Tier Experience

**Date:** 2026-06-08
**Status:** Design (approved, pending spec review and implementation plan)
**Branch:** `feature/extra-credit-implementation`
**Author:** opencode + collaboration with user
**Extra-credit task:** "Flujo condicional remoto con A/B testing" (Remote Config activates an experimental feature for a subset of users, a background service behaviour changes, results logged to Realtime Database).

---

## 1. Summary

Introduce a **plan-based variant system** in AURA. The user's plan (`Free` or `Premium`) is sourced from a single Remote Config parameter (`user_plan`) and exposed to the rest of the app as a typed `ExperimentVariant`. The variant drives three observable differences:

1. **Home dashboard cards** — Free shows 2 cards; Premium shows 3 (adds a "Daily Motivation" card).
2. **Bottom navigation tabs** — Free shows Home, Todos, Settings; Premium additionally shows Habits and Journal.
3. **Notification cadence & channels** — Free delivers 1 daily summary at the user-configured time on `CHANNEL_DAILY_SUMMARY`; Premium delivers 2 summaries (morning + evening) and, when a todo has a `dueDate` within 24h, also posts to `CHANNEL_DUE_DATE_REMINDER` (channel already exists but is unused).

A `CoroutineWorker` (`ExperimentsHeartbeatWorker`) runs every 12h and logs a single `session_active` event to Realtime Database at `users/{uid}/experiments/events/{pushId}`, including the current `variant`. Additional events (`home_opened`, `tab_clicked`, `notification_delivered`) are logged from the relevant ViewModels via the new `LogExperimentEventUseCase`.

A debug toggle in the Settings screen (Android-only, dev-friendly) lets the user switch between `Free` and `Premium` instantly to verify the UI without going through Firebase Console.

The architecture follows the existing Clean Architecture pattern (`domain/`, `data/`, `presentation/`, `di/`) used by `auth/`, `todo/`, `habit/`, and `notification/`. The new feature lives in a new module `experiments/` so it is isolated, mockable, and does not require touching the auth, todo, habit, or journal repositories.

---

## 2. Goals and non-goals

### 2.1 Goals

- Deliver a visible, demoable A/B experience where one user group sees 3 tabs / 2 Home cards / 1 daily notification, and another sees 5 tabs / 3 Home cards / 2 daily notifications + due-date reminders.
- Reuse existing infrastructure where possible: `FeatureFlagManager`, `WorkManager` (already has `DailySummaryWorker`), `NotificationHelper` (already has 2 channels), `DataStore` (for persisting the plan locally for instant UI updates), `DesignSystem` components.
- Add **exactly one** new Firebase dependency: `firebase-database-ktx` (for logging events to Realtime Database).
- Make the entire flow mockable through `Mockative` so it can be tested without a Firebase project.
- Provide a non-Firebase path (debug button in Settings) for local testing.

### 2.2 Non-goals

- **No** real payment / subscription integration. Plan is set manually via the debug button or via Remote Config in Firebase Console.
- **No** migration of Firestore-backed data.
- **No** Room / offline persistence (handled in a later extra-credit task).
- **No** iOS implementations beyond stubs (matches the pattern of all other platform-specific repositories).
- **No** analytics dashboards in the app — events are visible only in Firebase Console → Realtime Database.

---

## 3. Architecture

### 3.1 Module layout

All new code under `composeApp/src/commonMain/kotlin/com/programovil/aura/experiments/`:

```
experiments/
├── domain/
│   ├── model/
│   │   ├── UserPlan.kt                 (sealed: Free, Premium)
│   │   ├── HomeVariant.kt              (data class: showsDailyMotivation: Boolean)
│   │   ├── NotificationVariant.kt      (data class: timesPerDay: Int, tone: Tone, useDueDateChannel: Boolean)
│   │   ├── Tone.kt                     (enum: Gentle, Direct)
│   │   └── ExperimentEvent.kt          (sealed: HomeOpened, TabClicked, NotificationDelivered, SessionActive)
│   ├── repository/
│   │   ├── UserPlanRepository.kt       (interface + expect fun createUserPlanRepository())
│   │   └── ExperimentRepository.kt     (interface + expect fun createExperimentRepository())
│   └── usecase/
│       ├── GetUserPlanUseCase.kt
│       ├── GetHomeVariantUseCase.kt
│       ├── GetNotificationVariantUseCase.kt
│       └── LogExperimentEventUseCase.kt
├── di/
│   └── ExperimentsModule.kt
└── presentation/                       (intentionally empty in commonMain; UI integration is in HomeViewModel / SettingsViewModel / Worker)
```

Android-only actuals in `composeApp/src/androidMain/kotlin/com/programovil/aura/experiments/`:

```
androidMain/.../experiments/
├── data/
│   └── repository/
│       ├── DataStoreUserPlanRepositoryImpl.kt
│       └── FirebaseExperimentRepositoryImpl.kt
└── presentation/
    └── worker/
        └── ExperimentsHeartbeatWorker.kt
```

iOS-only stubs in `composeApp/src/iosMain/.../experiments/data/repository/`:

```
iosMain/.../experiments/
├── IosUserPlanRepositoryImpl.kt
└── IosExperimentRepositoryImpl.kt
```

### 3.2 Data flow

```
                ┌──────────────────────────────────────────┐
                │           Remote Config (Firebase)        │
                │   parameter: user_plan  (Free | Premium)  │
                └────────────────┬─────────────────────────┘
                                 │ getString(USER_PLAN_FLAG)
                                 ▼
                ┌──────────────────────────────────────────┐
                │ FirebaseRemoteConfigService (existing)    │
                │   → RemoteConfigService.getString()       │
                └────────────────┬─────────────────────────┘
                                 │
                                 ▼
        ┌────────────────────────────────────────────────┐
        │ GetUserPlanUseCase                              │
        │   1. Try local DataStore (instant)              │
        │   2. If absent, read Remote Config              │
        │   3. Persist to DataStore for next launch       │
        └─────┬────────────────────────────┬──────────────┘
              │                            │
              ▼                            ▼
   HomeViewModel                AndroidNotificationScheduler
   .homeVariant                 .scheduleDailySummary()
   → 2 or 3 cards               → 1 or 2 enqueued workers
              │                            │
              ▼                            ▼
   HomeScreen composable        DailySummaryWorker
   (conditional 3rd card)      (useDueDateChannel branch)
                                            │
                                            ▼
                                LogExperimentEventUseCase
                                            │
                                            ▼
                       ExperimentRepository.logEvent()
                                            │
                                            ▼
                       Realtime Database:
                       /users/{uid}/experiments/events/{pushId}
```

### 3.3 Plan source of truth

The plan is a single string (`"Free"` | `"Premium"`) flowing from Remote Config to DataStore to the rest of the app.

- **First launch**: `GetUserPlanUseCase` reads Remote Config (default `"Free"`), writes to DataStore, returns the value.
- **Subsequent launches**: DataStore is read first (instant), Remote Config is read in the background and DataStore is updated if changed.
- **Remote Config change (real-time)**: `FeatureFlagManager` already polls every 30s and listens for push updates. The `GetUserPlanUseCase` consumes the same `FeatureFlagManager.flags` flow indirectly by reading the `user_plan` parameter on each call (cached by Remote Config).
- **Debug button**: writes the plan to DataStore directly, bypassing Remote Config. The next call to `GetUserPlanUseCase` returns the new value immediately.

### 3.4 Realtime Database event logging

Event schema at `users/{uid}/experiments/events/{pushId}`:

```json
{
  "type": "home_opened" | "tab_clicked" | "notification_delivered" | "session_active",
  "variant": "Free" | "Premium",
  "timestamp": 1717850000000,
  "metadata": {
    "tab": "Todos"   // only for tab_clicked
  }
}
```

Index: `experiments/{eventType}/{yyyy-MM-dd}/count` (incremented from the client using `runTransaction` — simple counter, no Cloud Function required).

`pushId` is generated by `database.reference.push().key` (server-side timestamp key).

---

## 4. Component contracts

### 4.1 `UserPlan` (domain model)

```kotlin
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

### 4.2 `HomeVariant` (domain model)

```kotlin
data class HomeVariant(
    val showsDailyMotivation: Boolean,
    val dailyMotivationCopy: StringResKey  // sealed: Gentle, Direct (resolved in UI)
)
```

### 4.3 `NotificationVariant` (domain model)

```kotlin
data class NotificationVariant(
    val timesPerDay: Int,            // 1 or 2
    val tone: Tone,                  // Gentle or Direct
    val useDueDateChannel: Boolean
)
```

### 4.4 `UserPlanRepository` (interface in `commonMain`)

```kotlin
@Mockable
interface UserPlanRepository {
    fun observeUserPlan(): Flow<UserPlan>
    suspend fun getUserPlan(): UserPlan
    suspend fun setUserPlan(plan: UserPlan)        // debug button calls this
    suspend fun refreshFromRemote(): Result<Unit>  // fetches Remote Config, updates DataStore
}
expect fun createUserPlanRepository(remoteConfigService: RemoteConfigService): UserPlanRepository
```

### 4.5 `ExperimentRepository` (interface in `commonMain`)

```kotlin
@Mockable
interface ExperimentRepository {
    suspend fun logEvent(event: ExperimentEvent)
}
expect fun createExperimentRepository(remoteConfigService: RemoteConfigService): ExperimentRepository
```

### 4.6 Use cases

```kotlin
class GetUserPlanUseCase(private val repo: UserPlanRepository) {
    suspend operator fun invoke(): UserPlan = repo.getUserPlan()
    fun observe(): Flow<UserPlan> = repo.observeUserPlan()
}

class GetHomeVariantUseCase(private val planRepo: UserPlanRepository) {
    suspend operator fun invoke(): HomeVariant = when (planRepo.getUserPlan()) {
        UserPlan.Free -> HomeVariant(showsDailyMotivation = false, ...)
        UserPlan.Premium -> HomeVariant(showsDailyMotivation = true, ...)
    }
}

class GetNotificationVariantUseCase(private val planRepo: UserPlanRepository) {
    suspend operator fun invoke(): NotificationVariant = when (planRepo.getUserPlan()) {
        UserPlan.Free -> NotificationVariant(timesPerDay = 1, tone = Gentle, useDueDateChannel = false)
        UserPlan.Premium -> NotificationVariant(timesPerDay = 2, tone = Direct, useDueDateChannel = true)
    }
}

class LogExperimentEventUseCase(private val repo: ExperimentRepository) {
    suspend operator fun invoke(event: ExperimentEvent) = repo.logEvent(event)
}
```

### 4.7 `ExperimentsHeartbeatWorker`

```kotlin
class ExperimentsHeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val plan = getUserPlanUseCase()
        logExperimentEventUseCase(ExperimentEvent.SessionActive(plan))
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "experiments_heartbeat_work"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExperimentsHeartbeatWorker>(12, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
```

Scheduled from `AndroidApp.onCreate` (next to the existing FCM topic subscription).

---

## 5. UI integration

### 5.1 Bottom navigation (`App.kt`)

`App.kt` already gates `showTodos`/`showHabits`/`showJournals` via `FeatureFlagManager.flags`. We **add** a new derived value `showPremiumFeatures: Boolean` from the new `user_plan` flag (or, more cleanly, from `GetUserPlanUseCase.observe()`). Habits and Journal are rendered only when `showHabits && showPremiumFeatures`.

### 5.2 Home screen (`HomeScreen.kt`)

`HomeViewModel.uiState` gains a new field `homeVariant: HomeVariant`. The screen renders the existing two `DashboardCard`s, then conditionally a third "Daily Motivation" `DashboardCard` if `homeVariant.showsDailyMotivation`. The third card uses the existing `DashboardCard` composable (no new design system component required) and reads its copy from `strings.xml` (e.g. `home_dashboard_motivation_title`, `home_dashboard_motivation_subtitle`).

### 5.3 Notification scheduler (`AndroidNotificationScheduler.kt`)

`scheduleDailySummary(hour, minute)` reads the current `NotificationVariant` once at scheduling time. If `timesPerDay == 1`, behaviour is identical to today (single `PeriodicWorkRequest`). If `timesPerDay == 2`, it enqueues a second `OneTimeWorkRequest` (or, more accurately, **two** `PeriodicWorkRequest`s with `KEEP` and different initial delays — one for the morning hour, one for `hour + 12` hours). Both call the same `DailySummaryWorker`. The `DailySummaryWorker.doWork()` reads its own `NotificationVariant` and, if `useDueDateChannel == true`, additionally fires a `CHANNEL_DUE_DATE_REMINDER` notification for any todo with a `dueDate` within 24h.

**Crash safety**: if the plan is changed at runtime, the user can trigger a reschedule by toggling the time field in Settings (existing re-schedule path). The plan change itself does not auto-reschedule (avoids race conditions during the session).

### 5.4 Settings debug button (`SettingsScreen.kt`)

A new `PreferenceItem` "Simulate Plan" with two chip-style buttons: `Free` (selected by default) and `Premium`. Tapping one calls `viewModel.setSimulatedPlan(plan)` which writes to DataStore via `UserPlanRepository.setUserPlan(...)` and immediately re-emits the `uiState`. The visual state of the active chip is derived from `settingsViewModel.uiState.userPlan`.

The debug button is gated by `BuildConfig.DEBUG` (Android default) so it is invisible in release builds.

---

## 6. Dependencies

### 6.1 Added (one)

- `gradle/libs.versions.toml`:
  ```toml
  firebaseDatabase = "21.0.0"
  firebase-database-ktx = { module = "com.google.firebase:firebase-database-ktx", version.ref = "firebaseDatabase" }
  ```
- `composeApp/build.gradle.kts`: add `implementation(libs.firebase.database.ktx)` to the existing `firebase.bom` platform block.

### 6.2 Reused (no change)

- `RemoteConfigService`, `FeatureFlagManager` (existing).
- `WorkManager`, `NotificationHelper`, `NotificationScheduler` (existing).
- `DataStore` (existing `aura_preferences` — adding one new key `user_plan`).
- `AuthService` for `currentUser.uid` when logging events.
- `DesignSystem` components: `DashboardCard` (existing) for the 3rd Home card.
- `Koin` DI graph (existing).
- `Mockative` + `Turbine` test stack (existing).

---

## 7. Strings (new in `strings.xml`)

```xml
<string name="home_dashboard_motivation_title">DAILY MOTIVATION</string>
<string name="home_dashboard_motivation_subtitle">%1$s</string>
<string name="settings_simulate_plan">Simulate Plan</string>
<string name="settings_simulate_plan_free">Free</string>
<string name="settings_simulate_plan_premium">Premium</string>
```

All UI-visible strings pass through `stringResource(Res.string.*)` per AGENTS.md.

---

## 8. Testing strategy

### 8.1 Unit tests (added in `commonTest`)

- `GetUserPlanUseCaseTest` (Turbine + Mockative) — verifies DataStore-first read, fallback to Remote Config.
- `GetHomeVariantUseCaseTest` (Mockative) — `Free` returns `showsDailyMotivation = false`, `Premium` returns `true`.
- `GetNotificationVariantUseCaseTest` (Mockative) — same shape, distinct variants per plan.
- `LogExperimentEventUseCaseTest` (Mockative) — verifies `coVerify { repository.logEvent(any()) }.wasInvoked(exactly = 1)`.
- `ExperimentEventTest` — sealed-class completeness check.
- `UserPlanTest` — `fromRemoteConfigString` mapping (Free/Premium/unknown→Free).

### 8.2 Existing tests

- `./gradlew :composeApp:testDebugUnitTest` must show the **same** pass/fail count as `master` (10 pre-existing failures in `Habit`/`Onboarding`/`Todo` are unrelated to this task and will be left as-is, per "do not break what is not broken").

### 8.3 Verification commands (run before declaring done)

1. `./gradlew :composeApp:compileDebugKotlinAndroid` — must succeed.
2. `./gradlew :composeApp:compileCommonMainKotlinMetadata` — must succeed.
3. `./gradlew :composeApp:lintDebug` — must not produce new high-severity issues.
4. `./gradlew :composeApp:testDebugUnitTest` — diff of pass/fail counts.
5. Manual: `git diff master` review before commit.

### 8.4 Manual smoke test (user-driven, after install)

1. Open app → sign in → see 3 tabs (Free) by default.
2. Open Settings → scroll to "Simulate Plan" → tap "Premium".
3. Restart app (or reopen) → see 5 tabs.
4. Open Home → see 3rd "DAILY MOTIVATION" card.
5. Open Settings → Notifications → time picker → save → expect 2 daily notification work requests (visible via `adb shell dumpsys jobscheduler | grep aura`).
6. Add a todo with `dueDate` 1h from now → wait for the second worker tick → expect a `CHANNEL_DUE_DATE_REMINDER` notification.
7. Firebase Console → Realtime Database → `users/{uid}/experiments/events/` → see events with `variant: "Premium"`.

---

## 9. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Plan changes mid-session cause UI flicker | `GetUserPlanUseCase.observe()` returns a `StateFlow`; UI recomposes naturally. Tab list is computed once per recomposition. |
| Race condition: `scheduleDailySummary` reads stale plan | `AndroidNotificationScheduler.scheduleDailySummary` reads the plan inside the function (not cached). If the user changes the time, the reschedule reads the current plan. If they change the plan only (debug button), the next reschedule picks up the new plan. No auto-reschedule on plan change to avoid races. |
| Heartbeat worker duplicates work | `ExistingPeriodicWorkPolicy.KEEP` ensures one instance. |
| RTDB rules too permissive | Rules template provided in §10 restricts reads/writes to authenticated `uid`. |
| New `firebase-database-ktx` could conflict with `firebase-firestore`'s transitive `firebase-database-collection` | Versions checked — `firebaseDatabase = "21.0.0"` is compatible with the existing Firebase BOM `34.12.0`. The transitive `firebase-database-collection` is already present (used internally by Firestore); adding the parent `firebase-database` is additive, not conflicting. |
| Debug button visible in release | Gated by `BuildConfig.DEBUG`. |

---

## 10. Firebase Console configuration (user-side)

After the code is merged, the user must do the following once:

1. **Realtime Database**: create a database (location `us-central1`), set rules:
   ```json
   {
     "rules": {
       "users": {
         "$uid": {
           ".read": "$uid === auth.uid",
           ".write": "$uid === auth.uid"
         }
       }
     }
   }
   ```

2. **Remote Config** → Parameters:
   - Add parameter `user_plan`, type `String`, default value `"Free"`.
   - Add a condition `is_premium` (Audience: `premium_users` with user property `plan in ['premium']`).
   - On the `user_plan` parameter, set value `"Free"` for the default condition and `"Premium"` for `is_premium`.
   - Publish.

3. **Analytics Audiences** (optional, for production segmentation):
   - Create audience `premium_users` with user property `plan == premium`.
   - The code will set this property automatically: `FirebaseAnalytics.getInstance().setUserProperty("plan", "premium")` when the user switches to Premium.

4. (Testing-only) **Use the in-app debug button** (Settings → "Simulate Plan") to toggle plans without going through Console.

---

## 11. Out of scope (deferred)

- **A/B test analytics dashboard** in the app.
- **Real payment / subscription** (Google Play Billing).
- **Audiences auto-update** when the user upgrades/downgrades (the `setUserPlan` call will already trigger the user property set; Audiences have a 24-48h propagation delay in any case).
- **Experiments for other features** (e.g., theme preferences, onboarding length) — same pattern, but each is its own design doc.
