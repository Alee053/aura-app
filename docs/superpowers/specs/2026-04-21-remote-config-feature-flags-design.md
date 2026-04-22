# Firebase Remote Config Feature Flags — Design

**Date:** 2026-04-21
**Status:** Approved

## Overview

Add Firebase Remote Config integration to control which app features (habits, todos, notifications) are visible at runtime, gated at the navigation level on Android. iOS gracefully degrades to defaults.

## Architecture

```
com.programovil.aura/
├── shared/
│   ├── RemoteConfigService.kt     # Interface (commonMain)
│   └── FeatureFlags.kt            # Default values enum (commonMain)
└── di/
    └── InitKoin.kt                # Wire up RemoteConfigService
```

- **Interface in `commonMain`** — follows existing KMP pattern (like `AuthService`)
- **Android implementation** — `FirebaseRemoteConfigService` using Firebase Remote Config
- **iOS stub** — returns default values, no remote fetching
- **Startup gate** — flags fetched once at app launch, cached by Firebase SDK internally

## Feature Flags

```kotlin
enum class FeatureFlag(
    val key: String,
    val defaultValue: Boolean
) {
    HABITS_ENABLED("habits_enabled", true),
    TODOS_ENABLED("todos_enabled", true),
    NOTIFICATIONS_ENABLED("notifications_enabled", true),
}
```

## RemoteConfigService Interface

```kotlin
interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun getString(flag: FeatureFlag, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>
}
```

## Behavior

- **Feature disabled:** nav item hidden from bottom navigation completely
- **Feature enabled:** nav item shown as normal
- **Fetch failure:** falls back to defaults set via `setDefaultsAsync` (fail-open — features enabled by default)
- **Cached values:** Firebase SDK handles caching; `fetchAndActivate()` fetches only if stale

## Android Implementation

`FirebaseRemoteConfigService` uses:
- `Firebase.remoteConfig` from `com.google.firebase:firebase-config-ktx`
- `setDefaultsAsync()` with enum defaults on initialization
- `fetchAndActivate().await()` from `kotlinx-coroutines-play-services`

## Startup Flow

1. `FirebaseConfig.initialize()` already called on Android startup
2. `initializeFeatureFlags()` called once in `App.kt` composable
3. Result stored in `StateFlow<Map<FeatureFlag, Boolean>>`
4. `AppNavHost` observes flags and conditionally adds routes

## Dependencies

Add to `build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-config-ktx")
```

No new dependencies needed — `kotlinx-coroutines-play-services` already in use.

## Scope

- Android-only feature flags (iOS receives defaults)
- Startup gate only (flags fixed until next cold start)
- Navigation-level gating only (feature code unchanged)
- No A/B testing or rollout percentage (can be added later)