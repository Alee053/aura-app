# KMP Hygiene Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix structural KMP hygiene issues (broken iOS bootstrap, documentation lies, design-system violations) while leaving functional iOS stubs in place.

**Architecture:** Minimal surgical changes. iOS remains stubbed for functionality, but the bootstrap no longer crashes, docs tell the truth, and `composeApp` stops violating its own design-system rules.

**Tech Stack:** Kotlin Multiplatform, Koin, Compose Multiplatform, Firebase (Android only), DataStore KMP

---

## File Map

| File | Responsibility | Action |
|------|---------------|--------|
| `composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt` | iOS entry point | Add Koin bootstrap before ComposeUIViewController |
| `AGENTS.md` | Agent-facing architecture docs | Fix habit persistence claim |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt` | Settings UI | Replace 5 hardcoded `Color` hex values with design-system palette refs |
| `docs/IOS_DEFERRED.md` | Deferred-work checklist | New file listing every iOS stub and its future replacement |

---

### Task 1: Fix iOS Koin Bootstrap

**Files:**
- Modify: `composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt`

- [ ] **Step 1: Read the current file**

```bash
cat composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt
```
Expected content:
```kotlin
package com.programovil.aura

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

- [ ] **Step 2: Replace with Koin-aware bootstrap**

```kotlin
package com.programovil.aura

import androidx.compose.ui.window.ComposeUIViewController
import com.programovil.aura.di.getModules
import com.programovil.aura.shared.StubRemoteConfigService
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    startKoin {
        modules(getModules(StubRemoteConfigService()))
    }
    App()
}
```

- [ ] **Step 3: Verify iOS compilation still succeeds**

Run:
```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```
Expected: `BUILD SUCCESSFUL` (iOS targets are skipped on Linux, but the task should not fail; check for no compilation errors in the output)

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt
git commit -m "fix(ios): initialize Koin in MainViewController to prevent runtime crash"
```

---

### Task 2: Fix AGENTS.md Documentation Inaccuracy

**Files:**
- Modify: `AGENTS.md` (root of repo)

- [ ] **Step 1: Locate the false claim**

Search for this line in `AGENTS.md`:
```
│   ├── habit/                             # Habit feature - Room KMP backed
```

- [ ] **Step 2: Replace with accurate description**

Old:
```
│   ├── habit/                             # Habit feature - Room KMP backed
```
New:
```
│   ├── habit/                             # Habit feature - Firestore backed (iOS stubs)
```

Also locate:
```
### Local Persistence (Room KMP)
Defined in `commonMain` with `expect fun getHabitDatabaseBuilder()`.
- **Android**: Uses `AndroidSQLiteDriver`.
- **iOS**: Uses `BundledSQLiteDriver`.
```

Replace that entire block with:
```
### Local Persistence (Room KMP — not yet implemented)
Room KMP is listed in the tech stack but not currently used. Habit data is stored in Firestore on Android and stubbed on iOS. When local caching is added, the expected pattern is:
- `commonMain` declares `expect fun getHabitDatabaseBuilder()`.
- `androidMain` provides `AndroidSQLiteDriver`.
- `iosMain` provides `BundledSQLiteDriver`.
```

- [ ] **Step 3: Verify no other "Room KMP" false claims exist**

```bash
grep -n "Room KMP" AGENTS.md
```
Expected: only the newly updated paragraph.

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md
git commit -m "docs: correct habit persistence description from Room KMP to Firestore"
```

---

### Task 3: Remove Hardcoded Colors from SettingsScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt`

- [ ] **Step 1: Add palette imports**

Add these imports after the existing `com.programovil.aura.designsystem.theme.*` imports:
```kotlin
import com.programovil.aura.designsystem.theme.DarkPalette
import com.programovil.aura.designsystem.theme.GreenPalette
import com.programovil.aura.designsystem.theme.HighContrastPalette
import com.programovil.aura.designsystem.theme.PurplePalette
import com.programovil.aura.designsystem.theme.RedPalette
```

- [ ] **Step 2: Replace each hardcoded color pair**

Replace:
```kotlin
colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A237E), androidx.compose.ui.graphics.Color(0xFF6C5CE7)),
```
with:
```kotlin
colors = listOf(PurplePalette.background, PurplePalette.primary),
```

Replace:
```kotlin
colors = listOf(androidx.compose.ui.graphics.Color(0xFF0A2B23), androidx.compose.ui.graphics.Color(0xFF16A085)),
```
with:
```kotlin
colors = listOf(GreenPalette.background, GreenPalette.primary),
```

Replace:
```kotlin
colors = listOf(androidx.compose.ui.graphics.Color(0xFF2C0B0B), androidx.compose.ui.graphics.Color(0xFFB33939)),
```
with:
```kotlin
colors = listOf(RedPalette.background, RedPalette.primary),
```

Replace:
```kotlin
colors = listOf(androidx.compose.ui.graphics.Color(0xFF121212), androidx.compose.ui.graphics.Color(0xFFBB86EC)),
```
with:
```kotlin
colors = listOf(DarkPalette.background, DarkPalette.primary),
```

Replace:
```kotlin
colors = listOf(androidx.compose.ui.graphics.Color(0xFF000000), androidx.compose.ui.graphics.Color(0xFFFFFF00)),
```
with:
```kotlin
colors = listOf(HighContrastPalette.background, HighContrastPalette.primary),
```

- [ ] **Step 3: Verify no raw Color hex values remain in composeApp**

```bash
grep -rn "Color(0xFF" composeApp/src/commonMain/kotlin/
```
Expected: no matches.

- [ ] **Step 4: Build Android to ensure no regressions**

```bash
./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt
git commit -m "refactor(settings): replace hardcoded hex colors with design-system palette refs"
```

---

### Task 4: Create iOS Deferred-Work Checklist

**Files:**
- Create: `docs/IOS_DEFERRED.md`

- [ ] **Step 1: Create the checklist file**

```markdown
# iOS Deferred Work Checklist

> This document tracks every iOS stub in the codebase. **Do not start iOS feature work without consulting this list.**
> Last updated: 2026-05-13

## Bootstrap ✅ (Fixed)
- [x] Koin initialization in `MainViewController.kt`

## Authentication
- [ ] `IosAuthService` — currently returns `SignedOut` / `Error`
  - **Future work:** Integrate Firebase Auth iOS SDK or Apple Sign-In
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/auth/domain/IosAuthService.kt`

## Data / Repositories
- [ ] `IosTodoRepositoryImpl` — returns empty list, no-ops for add/toggle/delete
  - **Future work:** Integrate Firebase Firestore iOS SDK behind `TodoRepository` interface
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/todo/data/repository/TodoRepositoryImpl.kt`

- [ ] `IosHabitRepositoryImpl` — returns empty list, no-ops for all operations
  - **Future work:** Integrate Firebase Firestore iOS SDK behind `HabitRepository` interface
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`

## Notifications
- [ ] `IosNotificationScheduler` — all methods are empty TODOs
  - **Future work:** Implement iOS local notifications via `UNUserNotificationCenter`
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/notification/domain/IosNotificationScheduler.kt`

- [ ] `NotificationPermissionHandler.ios.kt` — always returns `hasPermission = true`
  - **Future work:** Request actual notification permission via `UNUserNotificationCenter`
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/presentation/NotificationPermissionHandler.ios.kt`

## Remote Config
- [ ] `StubRemoteConfigService` — returns default values, never fetches
  - **Future work:** Integrate Firebase Remote Config iOS SDK
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/shared/StubRemoteConfigService.kt`

## Firebase SDK Integration
- [ ] `iosApp/` Xcode project has no Firebase pods/SPM dependencies
  - **Future work:** Add Firebase iOS SDK via Swift Package Manager or CocoaPods
  - **Note:** Android uses `FirebaseConfig` object in `androidMain`. iOS needs equivalent initialization in Swift.

## Signing & Sign-In Callback
- [ ] `MainViewController.kt` calls `App()` without `onSignInClick`
  - **Future work:** Pass an iOS sign-in callback when Apple Sign-In is implemented
  - **File:** `composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt`
```

- [ ] **Step 2: Verify file was created correctly**

```bash
cat docs/IOS_DEFERRED.md | head -20
```
Expected: see the markdown header and first checklist items.

- [ ] **Step 3: Commit**

```bash
git add docs/IOS_DEFERRED.md
git commit -m "docs: add iOS deferred-work checklist"
```

---

## Self-Review

- [x] **Spec coverage:** All three hygiene issues from the audit are covered (Koin init, docs, colors) plus the deferred checklist.
- [x] **Placeholder scan:** No TBD, TODO, or vague steps. Every edit shows exact code.
- [x] **Type consistency:** Palette names (`PurplePalette`, `GreenPalette`, etc.) match the public vals in `designsystem/theme/Color.kt`. Koin API (`startKoin { modules(...) }`) matches Koin 4.x used in the project.
- [x] **Regression check:** Task 3 includes a build step to verify SettingsScreen changes don't break Android compilation.
