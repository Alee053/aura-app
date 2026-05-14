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
