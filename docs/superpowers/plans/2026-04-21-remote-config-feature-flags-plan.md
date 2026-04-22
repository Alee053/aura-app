# Firebase Remote Config Feature Flags — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Android-only feature flags via Firebase Remote Config to show/hide navigation items at startup.

**Architecture:** Interface in `commonMain`, Firebase implementation in `androidMain`, stub in `iosMain`. Flags fetched once at app launch and stored in a `StateFlow` consumed by the navigation bar.

**Tech Stack:** Kotlin Multiplatform, Koin DI, Firebase Remote Config, kotlinx-coroutines-play-services

---

## File Structure

```
composeApp/src/commonMain/kotlin/com/programovil/aura/
├── shared/
│   ├── FeatureFlags.kt              # New: enum with flag definitions
│   ├── RemoteConfigService.kt       # New: interface
│   └── FeatureFlagManager.kt        # New: holds StateFlow of flags
├── di/
│   └── InitKoin.kt                  # Modify: add remote config to Koin
└── App.kt                           # Modify: initialize flags, conditional nav

composeApp/src/androidMain/kotlin/com/programovil/aura/
└── shared/
    └── FirebaseRemoteConfigService.kt  # New: Firebase impl

composeApp/src/iosMain/kotlin/com/programovil/aura/
└── shared/
    └── StubRemoteConfigService.kt       # New: iOS stub impl

composeApp/
└── build.gradle.kts                    # Modify: add firebase-config-ktx dep
```

---

## Dependencies

### Task 0: Add Firebase Remote Config dependency

**Files:**
- Modify: `composeApp/build.gradle.kts:42-45`

- [ ] **Step 1: Add firebase-config-ktx to androidMain dependencies**

In `composeApp/build.gradle.kts`, after line 45 (`implementation(libs.firebase.messaging.ktx)`), add:
```kotlin
implementation("com.google.firebase:firebase-config-ktx")
```

---

## Shared Interface & Models

### Task 1: FeatureFlags enum

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt`

- [ ] **Step 1: Write FeatureFlags enum**

```kotlin
package com.programovil.aura.shared

enum class FeatureFlag(
    val key: String,
    val defaultValue: Boolean
) {
    HABITS_ENABLED("habits_enabled", true),
    TODOS_ENABLED("todos_enabled", true),
    NOTIFICATIONS_ENABLED("notifications_enabled", true),
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlags.kt
git commit -m "feat: add FeatureFlags enum with habit/todo/notification keys"
```

---

### Task 2: RemoteConfigService interface

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt`

- [ ] **Step 1: Write RemoteConfigService interface**

```kotlin
package com.programovil.aura.shared

interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun getString(flag: FeatureFlag, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/RemoteConfigService.kt
git commit -m "feat: add RemoteConfigService interface"
```

---

### Task 3: FeatureFlagManager

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt`

- [ ] **Step 1: Write FeatureFlagManager**

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
        remoteConfigService.fetchAndActivate()
        val updatedFlags = FeatureFlag.entries.associateWith { flag ->
            remoteConfigService.getBoolean(flag)
        }
        _flags.value = updatedFlags
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/shared/FeatureFlagManager.kt
git commit -m "feat: add FeatureFlagManager with StateFlow"
```

---

## Android Implementation

### Task 4: FirebaseRemoteConfigService

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt`

- [ ] **Step 1: Write FirebaseRemoteConfigService**

```kotlin
package com.programovil.aura.shared

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.setDefaultsAsync
import kotlinx.coroutines.tasks.await

class FirebaseRemoteConfigService(context: Context) : RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig

    init {
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
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/shared/FirebaseRemoteConfigService.kt
git commit -m "feat: add FirebaseRemoteConfigService for Android"
```

---

## iOS Stub Implementation

### Task 5: StubRemoteConfigService

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt`

- [ ] **Step 1: Write StubRemoteConfigService**

```kotlin
package com.programovil.aura.shared

class StubRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(flag: FeatureFlag): Boolean = flag.defaultValue
    override suspend fun getString(flag: FeatureFlag, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt
git commit -m "feat: add StubRemoteConfigService for iOS"
```

---

## Koin DI Wiring

### Task 6: Update AndroidApp and InitKoin

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`

**Approach:** `FirebaseRemoteConfigService` is instantiated in `AndroidApp.onCreate()` before Koin starts, then registered as a singleton in Koin. `FeatureFlagManager` is also registered. This avoids Android-only code in `commonMain`.

- [ ] **Step 1: Update InitKoin.kt to accept RemoteConfigService and FeatureFlagManager as parameters**

```kotlin
package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.habit.di.habitModule
import com.programovil.aura.notification.di.notificationModule
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.shared.RemoteConfigService
import com.programovil.aura.todo.di.todoModule
import org.koin.dsl.module

fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    module {
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

- [ ] **Step 2: Update AndroidApp.kt to create FirebaseRemoteConfigService and pass it to Koin**

```kotlin
class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseConfig.initialize(this)
        FirebaseConfig.messaging.subscribeToTopic("test-notifications")
            .addOnCompleteListener { }
        NotificationHelper.createNotificationChannels(this)

        val remoteConfigService = FirebaseRemoteConfigService(this)

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules(remoteConfigService))
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git add composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt
git commit -m "feat: wire RemoteConfigService and FeatureFlagManager into Koin"
```

---

## App Initialization with Conditional Navigation

### Task 7: Modify App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt:72-136`

- [ ] **Step 1: Add FeatureFlagManager initialization and conditional nav items**

In `AuthenticatedApp`, add:
```kotlin
val featureFlagManager: FeatureFlagManager = koinInject()
val featureFlags by featureFlagManager.flags.collectAsState()
```

In `Scaffold`'s `NavigationBar`, wrap each `NavigationBarItem` in a conditional:
```kotlin
val showTodos by remember(featureFlags) { 
    mutableStateOf(featureFlags[FeatureFlag.TODOS_ENABLED] ?: true) 
}
val showHabits by remember(featureFlags) { 
    mutableStateOf(featureFlags[FeatureFlag.HABITS_ENABLED] ?: true) 
}
val showNotifications by remember(featureFlags) { 
    mutableStateOf(featureFlags[FeatureFlag.NOTIFICATIONS_ENABLED] ?: true) 
}
```

Then wrap nav items:
```kotlin
if (showTodos) {
    NavigationBarItem(/* Todo item */)
}
if (showHabits) {
    NavigationBarItem(/* Habit item */)
}
if (showNotifications) {
    NavigationBarItem(/* Notification item */)
}
```

- [ ] **Step 2: Launch initialization in LaunchedEffect**

Add to `AuthenticatedApp`:
```kotlin
LaunchedEffect(Unit) {
    featureFlagManager.initialize()
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "feat: add conditional navigation based on feature flags"
```

---

## Verification

### Task 8: Verify build

**Files:**
- None (verification only)

- [ ] **Step 1: Run Android build**

```bash
./gradlew :composeApp:assembleDebug 2>&1 | tail -50
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify no new compilation errors**