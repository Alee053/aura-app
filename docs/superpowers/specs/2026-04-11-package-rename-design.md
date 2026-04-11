# Package Rename Design Spec

## Date: 2026-04-11

## Problem

The app crashes on startup due to two misconfigurations:

1. **Package mismatch**: All Kotlin source files declare `org.example.aura_app` but `google-services.json` and Gradle config use `com.programovil.aura`.
2. **Hardcoded OAuth client ID**: `MainActivity.kt` has a Google Sign-In token registered for `org.example.aura_app`, which is incompatible with `com.programovil.aura`.

## Solution: Rename to `com.programovil.aura`

### What changes

| # | File | Change |
|---|------|--------|
| 1 | All directories under `src/*/kotlin/org/example/aura_app/` | Rename to `com/programovil/aura/` |
| 2 | All `package org.example.aura_app` declarations | → `package com.programovil.aura` |
| 3 | All `import org.example.aura_app.*` statements | → `import com.programovil.aura.*` |
| 4 | `MainActivity.kt` — hardcoded `.requestIdToken()` token | Replace with new OAuth client ID for `com.programovil.aura` |
| 5 | `AndroidApp.kt` — `org.example.aura_app.data.remote.FirebaseConfig` import | Update |
| 6 | `MainActivity.kt` — `org.example.aura_app.presentation.auth.AuthViewModel` import | Update |
| 7 | `App.kt` — all `org.example.aura_app` imports | Update |
| 8 | `AppModules.kt` — all `org.example.aura_app` imports | Update |
| 9 | `TodoRepositoryImpl.kt`, `TodoRepository.kt`, `TodoViewModel.kt`, `TodoScreen.kt`, `NavRoutes.kt`, `AppNavHost.kt`, `Platform.android.kt`, `Platform.ios.kt`, `MainViewController.kt`, `Todo.kt` | Update package + imports |
| 10 | `google-services.json` | *(already correct — `com.programovil.aura`)* |

### Steps to generate new OAuth client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**
2. Select the Web application OAuth client used for Sign-In
3. Update the "Authorized package name" field for Android to `com.programovil.aura`, OR create a new OAuth 2.0 Client ID for Android with package `com.programovil.aura` and SHA-1 from your debug keystore
4. Copy the new client ID string (e.g., `123...apps.googleusercontent.com`)
5. Replace the value in `MainActivity.kt` line 42

### After the rename

- Verify `adb shell pm dump com.programovil.aura | grep versionName` returns `1.0`
- Run the app and confirm it starts without crashing
- Test Google Sign-In flow

## Files affected

```
composeApp/src/androidMain/kotlin/org/example/aura_app/
  → composeApp/src/androidMain/kotlin/com/programovil/aura/

composeApp/src/commonMain/kotlin/org/example/aura_app/
  → composeApp/src/commonMain/kotlin/com/programovil/aura/

composeApp/src/iosMain/kotlin/org/example/aura_app/
  → composeApp/src/iosMain/kotlin/com/programovil/aura/
```

Total files to move and update: ~15 Kotlin source files + directory structure.

## Verification checklist

- [ ] All source files moved to new directory structure
- [ ] All package declarations updated
- [ ] All imports updated
- [ ] `MainActivity.kt` OAuth client ID updated
- [ ] App builds successfully (`./gradlew :composeApp:assembleDebug`)
- [ ] App launches without crash
- [ ] Google Sign-In completes successfully
