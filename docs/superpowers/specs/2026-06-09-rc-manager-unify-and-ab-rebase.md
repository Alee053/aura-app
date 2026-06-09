# Remote Config Manager Unification + A/B Experiments Re-base

**Date:** 2026-06-09
**Branch:** `refactor/extra-credit-ab-testing` (worktree at `.worktrees/extra-credit-ab-testing/`)
**Tracks:** PR #12 (`feature/extra-credit-implementation`)
**Status:** Draft, awaiting review

---

## Problem

PR #12 introduces an A/B testing / Free vs Premium feature on top of the existing remote-config feature-flag system, but several things collide with `master`:

1. **Duplicated managers.** `FeatureFlagManager` and `UserPlanManager` are ~80 lines each, with the same fetch / listener / 30s-polling / `StateFlow` / `stop()` shape. Adding a third config value would copy-paste the same code a third time.
2. **Leaky interface.** `RemoteConfigService` has both `getString(flag: FeatureFlag, default: String)` (typed to `FeatureFlag`) and a special-case `getUserPlan()`. The second method is specific to one flag and breaks the generic "give me a key" pattern.
3. **Polling is redundant.** Three managers, each running a 30-second poll on top of Firebase's real-time channel. The listener + on-resume refresh is enough.
4. **Feature flag vs user plan precedence is implicit.** Both signals want to gate features (the PR gates the Habits tab on `HABITS_ENABLED && userPlan == Premium`). The rule is in `App.kt`, not in domain. Easy to forget, hard to test.
5. **Wrong Firebase project.** PR #12 swaps `google-services.json` to a different Firebase project (`aura-6ac09` vs `aura-app-7dce3`) and changes `MainActivity`'s Google client ID. This breaks existing auth/sessions for any device linked to the master project.

## Goal

Land the A/B experiments from PR #12 on top of `master` with the Firebase project config restored, after a small refactor that:

- Unifies remote-config value management into one generic core.
- Generalizes `RemoteConfigService` to a key-based interface.
- Establishes a single domain use case as the source of truth for "is this feature accessible to this user?".

Non-goals: introducing new features, changing the A/B experiment semantics, expanding to iOS Firebase, removing the DataStore override layer for `UserPlan`.

---

## Design

### 1. Generic core: `RemoteConfigValueManager<T>`

New class in `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/`.

```kotlin
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

- **No polling loop.** Firebase's real-time `addOnConfigUpdateListener` is the only source of updates.
- **One scope per manager.** `stop()` cancels the scope. Callers (UI lifecycle) decide when to stop.
- **Parser is a constructor argument.** Keeps the manager type-safe; bad parses fall back to the default.
- `refresh()` is `public` so consumers can re-fetch on resume if they want to (not required by the listener, but available).
- **Lifecycle contract**: callers must call `initialize()` once at startup before consuming `value`. After `stop()`, the manager must not be used again. Calling `refresh()` after `stop()` is a silent no-op (the scope is cancelled). Calling `refresh()` before `initialize()` is permitted but the fetch result is not reflected in `value` until the listener is registered — in practice this is only useful inside `initialize()` itself, so the public API is `initialize()` then read `value`.

### 2. Typed wrappers

`FeatureFlagManager` and `UserPlanManager` are thin facades that compose one or more `RemoteConfigValueManager<T>` instances and expose the existing public API. **No logic change for consumers.**

```kotlin
class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val managers: Map<FeatureFlag, RemoteConfigValueManager<Boolean>> =
        FeatureFlag.entries.associateWith { flag ->
            RemoteConfigValueManager(
                remoteConfigService = remoteConfigService,
                key = flag.key,
                defaultValue = flag.defaultValue,
                parser = { it.toBooleanStrictOrNull() ?: flag.defaultValue }
            )
        }

    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        managers.values.forEach { it.initialize() }
        managers.forEach { (flag, mgr) ->
            scope.launch { mgr.value.collect { _flags.update { it + (flag to value) } } }
        }
    }

    fun stop() { managers.values.forEach { it.stop() } }
}
```

`UserPlanManager` mirrors the same shape, with a `RemoteConfigValueManager<UserPlan>` whose parser maps the RC string to the `UserPlan` enum.

### 3. `RemoteConfigService` interface (key-based, generic)

```kotlin
interface RemoteConfigService {
    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun getString(key: String, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>
    fun registerOnConfigUpdateListener(onUpdate: () -> Unit)
}
```

- `getUserPlan()` is **removed**.
- `getString(flag: FeatureFlag, default: String)` is **removed**; callers use `getString(flag.key, default)` instead.
- `getBoolean(flag: FeatureFlag)` is **removed**; callers use `getBoolean(flag.key, flag.defaultValue)` instead. This affects `FeatureFlagManager` only, not the wider app (no other consumers exist today).

**Platform impls**:
- `FirebaseRemoteConfigService` (Android) — keep `minimumFetchIntervalInSeconds = 0` for debug. Add the new `UserPlanFlag.USER_PLAN.key` to the defaults map. No other behavior change.
- `StubRemoteConfigService` (iOS) — return defaults for all keys. No behavior change beyond the interface refactor.

### 4. Domain use case for access decisions

`GetHabitsAccessibilityUseCase` (new) in `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/`:

```kotlin
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

- Single source of truth for "can this user see the Habits feature?".
- `App.kt` consumes this flow to gate the bottom-nav item and the route inside `AppNavHost`.
- The same pattern can be extended to Journal or other future premium-gated features (none right now).

The PR's existing use cases (`GetUserPlanUseCase`, `GetHomeVariantUseCase`, `GetNotificationVariantUseCase`, `LogExperimentEventUseCase`) are **unchanged**.

### 5. Data flow

```
[Remote Config] → RemoteConfigService (Android: Firebase; iOS: stub)
                         ↓
            RemoteConfigValueManager<T>  (one per logical value)
                         ↓
            ┌────────────┴────────────┐
            ↓                         ↓
   FeatureFlagManager          UserPlanManager
   (Map<FeatureFlag, Boolean>)  (UserPlan)
            ↓                         ↓
            └──────────┬──────────────┘
                       ↓
        GetHabitsAccessibilityUseCase
                       ↓
                     UI
```

A single real-time update from Firebase → `RemoteConfigService.registerOnConfigUpdateListener` callback → each `RemoteConfigValueManager.refresh()` → StateFlow emit → all downstream `combine`s re-evaluate → UI recomposes.

### 6. Error handling

- `fetchAndActivate()` already returns `Result<Unit>`; failures keep the last known value.
- Parser failures (e.g., Remote Config returns `"unknown"` for `user_plan`) fall back to the default; a warning is logged.
- `addOnConfigUpdateListener.onError` is logged; the next update or app restart recovers.
- `stop()` is idempotent. Calling it twice is a no-op.

### 7. Testing

**New tests** (`commonTest`):
- `RemoteConfigValueManagerTest` — parser fallback, refresh after listener callback, stop() cancels scope, idempotent init.
- `UserPlanManagerTest` — mirrors `FeatureFlagManagerTest` for the `UserPlan` shape.
- `GetHabitsAccessibilityUseCaseTest` — table-driven: `(flag, plan) → expected Boolean`.

**Updated tests**:
- `FeatureFlagManagerTest` — adapt to the new single-manager shape, no polling.

**Unchanged**: all existing experiments tests (`GetUserPlanUseCaseTest`, `GetHomeVariantUseCaseTest`, `GetNotificationVariantUseCaseTest`, `LogExperimentEventUseCaseTest`, `UserPlanTest`, `ExperimentEventTest`).

### 8. Restore order (concrete steps)

The refactor is applied on top of the PR, but **before** the refactor we revert the PR's Firebase project config:

1. **Revert** `composeApp/google-services.json` to `master`'s `aura-app-7dce3` project.
2. **Revert** the Google client ID in `MainActivity.kt` back to `master`'s `623141386052-...` value.
3. **Revert** any other Firebase project-id changes in `build.gradle.kts` or other files (none expected, but check).
4. **Apply the manager refactor** on the cleaned-up branch (this design).
5. **Re-apply the experiments code** on the refactored foundation. Most of PR #12 survives unchanged; only the manager-touching parts need rewriting.
6. **Verify** with `./gradlew :composeApp:assembleDebug` and the unit tests.

Result: a single clean PR with the refactor + experiments + restored project config. The original `feature/extra-credit-implementation` branch is left in place; this new branch supersedes it.

---

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Removing `getString(flag: FeatureFlag, default)` breaks existing call sites | Only the managers use it today; both will be updated atomically with the refactor. |
| Real-time listener can be silently dropped by Firebase (network blip) | `refresh()` is public; a one-shot call from a `LaunchedEffect(lifecycle)` in `App.kt` recovers on resume. No background poll. |
| `getString` returns the same raw key for flags that happen to be strings (none today) | Future risk; if it materializes, add a typed overload or a different manager. Out of scope now. |
| iOS stub never refreshes | iOS is out of scope for A/B events. The Home variant and Habits accessibility still work on iOS — they just never see a non-default value. Documented. |
| Refactor changes the public API of `FeatureFlagManager` | The only consumer is `App.kt`. Update it in the same PR. |

## Out of scope

- iOS Firebase integration.
- Polling-based fallback (intentionally removed; real-time + on-resume is enough).
- Removing the `DataStoreUserPlanRepositoryImpl` override layer (kept for future payment flow).
- A `GetJournalAccessibilityUseCase` (no current consumer).
- Changing the RTDB event schema or `ExperimentsHeartbeatWorker` cadence.
- Per-build-type `minimumFetchIntervalInSeconds` (debug vs release). Could be added later.
