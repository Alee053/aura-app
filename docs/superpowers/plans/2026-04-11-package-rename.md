# Package Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename all Kotlin source packages from `org.example.aura_app` to `com.programovil.aura` to match `google-services.json` and fix the OAuth client ID mismatch causing the crash.

**Architecture:** Mechanical package rename across all Kotlin source files in a Kotlin Multiplatform project. Directories and package declarations are updated together, then a Gradle build verifies correctness.

**Tech Stack:** Kotlin Multiplatform, Koin DI, Firebase Auth/Firestore, Google Sign-In

---

## File Structure

**Target directories (create these):**
```
composeApp/src/androidMain/kotlin/com/programovil/aura/
composeApp/src/commonMain/kotlin/com/programovil/aura/
composeApp/src/iosMain/kotlin/com/programovil/aura/
```

**Source directories (delete after moving):**
```
composeApp/src/androidMain/kotlin/org/example/aura_app/
composeApp/src/commonMain/kotlin/org/example/aura_app/
composeApp/src/iosMain/kotlin/org/example/aura_app/
```

**Files to move (15 total):**

| File | Source Path | Target Path |
|------|-------------|-------------|
| AndroidApp.kt | `androidMain/kotlin/org/example/aura_app/` | `androidMain/kotlin/com/programovil/aura/` |
| MainActivity.kt | `androidMain/kotlin/org/example/aura_app/` | `androidMain/kotlin/com/programovil/aura/` |
| Platform.android.kt | `androidMain/kotlin/org/example/aura_app/` | `androidMain/kotlin/com/programovil/aura/` |
| App.kt | `commonMain/kotlin/org/example/aura_app/` | `commonMain/kotlin/com/programovil/aura/` |
| AppModules.kt | `commonMain/kotlin/org/example/aura_app/di/` | `commonMain/kotlin/com/programovil/aura/di/` |
| FirebaseConfig.kt | `commonMain/kotlin/org/example/aura_app/data/remote/` | `commonMain/kotlin/com/programovil/aura/data/remote/` |
| TodoRepositoryImpl.kt | `commonMain/kotlin/org/example/aura_app/data/repository/` | `commonMain/kotlin/com/programovil/aura/data/repository/` |
| Todo.kt | `commonMain/kotlin/org/example/aura_app/domain/model/` | `commonMain/kotlin/com/programovil/aura/domain/model/` |
| TodoRepository.kt | `commonMain/kotlin/org/example/aura_app/domain/repository/` | `commonMain/kotlin/com/programovil/aura/domain/repository/` |
| AuthViewModel.kt | `commonMain/kotlin/org/example/aura_app/presentation/auth/` | `commonMain/kotlin/com/programovil/aura/presentation/auth/` |
| NavRoutes.kt | `commonMain/kotlin/org/example/aura_app/presentation/navigation/` | `commonMain/kotlin/com/programovil/aura/presentation/navigation/` |
| AppNavHost.kt | `commonMain/kotlin/org/example/aura_app/presentation/navigation/` | `commonMain/kotlin/com/programovil/aura/presentation/navigation/` |
| TodoScreen.kt | `commonMain/kotlin/org/example/aura_app/presentation/todo/` | `commonMain/kotlin/com/programovil/aura/presentation/todo/` |
| TodoViewModel.kt | `commonMain/kotlin/org/example/aura_app/presentation/todo/` | `commonMain/kotlin/com/programovil/aura/presentation/todo/` |
| Platform.ios.kt | `iosMain/kotlin/org/example/aura_app/` | `iosMain/kotlin/com/programovil/aura/` |
| MainViewController.kt | `iosMain/kotlin/org/example/aura_app/` | `iosMain/kotlin/com/programovil/aura/` |

**Files with package declaration to update (replace `org.example.aura_app` → `com.programovil.aura`):**
All 16 files above.

**Files with import statements to update (replace `org.example.aura_app` → `com.programovil.aura`):**
- `AndroidApp.kt`
- `MainActivity.kt`
- `App.kt`
- `AppModules.kt`
- `FirebaseConfig.kt`
- `TodoRepositoryImpl.kt`
- `AuthViewModel.kt`
- `NavRoutes.kt`
- `AppNavHost.kt`
- `TodoScreen.kt`
- `TodoViewModel.kt`

**Files with OAuth client ID to update:**
- `MainActivity.kt` — line 42: `.requestIdToken("623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com")`

---

## Important: OAuth Client ID

**⚠️ You must provide the new OAuth client ID before Task 4.**

The current ID in `MainActivity.kt:42` is for `org.example.aura_app` and will not work with `com.programovil.aura`. To get a new client ID:

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**
2. Open the existing OAuth 2.0 Client ID for the Android app
3. Update the package name to `com.programovil.aura` (or create a new one)
4. Get your debug SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
5. Copy the new client ID string (format: `123...apps.googleusercontent.com`)

---

## Tasks

### Task 1: Create new directory structure

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/`
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/{data/remote,data/repository,di,domain/model,domain/repository,presentation/auth,presentation/navigation,presentation/todo}/`

- [ ] **Step 1: Create top-level package directories**

```bash
mkdir -p composeApp/src/androidMain/kotlin/com/programovil/aura
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura
mkdir -p composeApp/src/iosMain/kotlin/com/programovil/aura
```

- [ ] **Step 2: Create subdirectories for commonMain**

```bash
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/data/remote
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/data/repository
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/di
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/domain/model
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/domain/repository
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/auth
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/navigation
mkdir -p composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/todo
```

- [ ] **Step 3: Commit — "chore: create new package directory structure for com.programovil.aura"**

---

### Task 2: Move and update androidMain files

**Files:**
- Move: `androidMain/kotlin/org/example/aura_app/AndroidApp.kt` → `androidMain/kotlin/com/programovil/aura/AndroidApp.kt`
- Move: `androidMain/kotlin/org/example/aura_app/MainActivity.kt` → `androidMain/kotlin/com/programovil/aura/MainActivity.kt`
- Move: `androidMain/kotlin/org/example/aura_app/Platform.android.kt` → `androidMain/kotlin/com/programovil/aura/Platform.android.kt`

- [ ] **Step 1: Move the three androidMain files to new directory**

```bash
mv composeApp/src/androidMain/kotlin/org/example/aura_app/AndroidApp.kt \
   composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt

mv composeApp/src/androidMain/kotlin/org/example/aura_app/MainActivity.kt \
   composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt

mv composeApp/src/androidMain/kotlin/org/example/aura_app/Platform.android.kt \
   composeApp/src/androidMain/kotlin/com/programovil/aura/Platform.android.kt
```

- [ ] **Step 2: Update package declaration in AndroidApp.kt**

In `composeApp/src/androidMain/kotlin/com/programovil/aura/AndroidApp.kt`:
- `package org.example.aura_app` → `package com.programovil.aura`
- `import org.example.aura_app.data.remote.FirebaseConfig` → `import com.programovil.aura.data.remote.FirebaseConfig`
- `import org.example.aura_app.di.getModules` → `import com.programovil.aura.di.getModules`

- [ ] **Step 3: Update package declaration in MainActivity.kt**

In `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt`:
- `package org.example.aura_app` → `package com.programovil.aura`
- `import org.example.aura_app.presentation.auth.AuthViewModel` → `import com.programovil.aura.presentation.auth.AuthViewModel`

- [ ] **Step 4: Update package declaration in Platform.android.kt**

In `composeApp/src/androidMain/kotlin/com/programovil/aura/Platform.android.kt`:
- `package org.example.aura_app` → `package com.programovil.aura`

- [ ] **Step 5: Remove old androidMain directory**

```bash
rmdir composeApp/src/androidMain/kotlin/org/example/aura_app
```

- [ ] **Step 6: Commit — "refactor: move androidMain to com.programovil.aura package"**

---

### Task 3: Move and update commonMain files

**Files:**
- Move: `commonMain/kotlin/org/example/aura_app/App.kt` → `commonMain/kotlin/com/programovil/aura/App.kt`
- Move: `commonMain/kotlin/org/example/aura_app/di/AppModules.kt` → `commonMain/kotlin/com/programovil/aura/di/AppModules.kt`
- Move: `commonMain/kotlin/org/example/aura_app/data/remote/FirebaseConfig.kt` → `commonMain/kotlin/com/programovil/aura/data/remote/FirebaseConfig.kt`
- Move: `commonMain/kotlin/org/example/aura_app/data/repository/TodoRepositoryImpl.kt` → `commonMain/kotlin/com/programovil/aura/data/repository/TodoRepositoryImpl.kt`
- Move: `commonMain/kotlin/org/example/aura_app/domain/model/Todo.kt` → `commonMain/kotlin/com/programovil/aura/domain/model/Todo.kt`
- Move: `commonMain/kotlin/org/example/aura_app/domain/repository/TodoRepository.kt` → `commonMain/kotlin/com/programovil/aura/domain/repository/TodoRepository.kt`
- Move: `commonMain/kotlin/org/example/aura_app/presentation/auth/AuthViewModel.kt` → `commonMain/kotlin/com/programovil/aura/presentation/auth/AuthViewModel.kt`
- Move: `commonMain/kotlin/org/example/aura_app/presentation/navigation/NavRoutes.kt` → `commonMain/kotlin/com/programovil/aura/presentation/navigation/NavRoutes.kt`
- Move: `commonMain/kotlin/org/example/aura_app/presentation/navigation/AppNavHost.kt` → `commonMain/kotlin/com/programovil/aura/presentation/navigation/AppNavHost.kt`
- Move: `commonMain/kotlin/org/example/aura_app/presentation/todo/TodoScreen.kt` → `commonMain/kotlin/com/programovil/aura/presentation/todo/TodoScreen.kt`
- Move: `commonMain/kotlin/org/example/aura_app/presentation/todo/TodoViewModel.kt` → `commonMain/kotlin/com/programovil/aura/presentation/todo/TodoViewModel.kt`

- [ ] **Step 1: Move all commonMain files using find + mv**

```bash
mv composeApp/src/commonMain/kotlin/org/example/aura_app/App.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/di/AppModules.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/di/AppModules.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/data/remote/FirebaseConfig.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/data/remote/FirebaseConfig.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/data/repository/TodoRepositoryImpl.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/data/repository/TodoRepositoryImpl.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/domain/model/Todo.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/domain/model/Todo.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/domain/repository/TodoRepository.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/domain/repository/TodoRepository.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/auth/AuthViewModel.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/auth/AuthViewModel.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/navigation/NavRoutes.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/navigation/NavRoutes.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/navigation/AppNavHost.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/navigation/AppNavHost.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoScreen.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/todo/TodoScreen.kt

mv composeApp/src/commonMain/kotlin/org/example/aura_app/presentation/todo/TodoViewModel.kt \
   composeApp/src/commonMain/kotlin/com/programovil/aura/presentation/todo/TodoViewModel.kt
```

- [ ] **Step 2: Update package and import declarations in all commonMain files**

Use `replace_all` in each file to change:
- `org.example.aura_app` → `com.programovil.aura` (package declarations)
- `org.example.aura_app` → `com.programovil.aura` (import statements)

Files to update:
1. `App.kt` — `package org.example.aura_app` → `package com.programovil.aura`; all imports
2. `AppModules.kt` — `package org.example.aura_app` → `package com.programovil.aura`; all imports
3. `FirebaseConfig.kt` — `package org.example.aura_app` → `package com.programovil.aura`
4. `TodoRepositoryImpl.kt` — `package org.example.aura_app` → `package com.programovil.aura`; imports
5. `Todo.kt` — `package org.example.aura_app` → `package com.programovil.aura`
6. `TodoRepository.kt` — `package org.example.aura_app` → `package com.programovil.aura`
7. `AuthViewModel.kt` — `package org.example.aura_app` → `package com.programovil.aura`
8. `NavRoutes.kt` — `package org.example.aura_app` → `package com.programovil.aura`
9. `AppNavHost.kt` — `package org.example.aura_app` → `package com.programovil.aura`; imports
10. `TodoScreen.kt` — `package org.example.aura_app` → `package com.programovil.aura`; imports
11. `TodoViewModel.kt` — `package org.example.aura_app` → `package com.programovil.aura`; imports

- [ ] **Step 3: Remove old commonMain directory tree**

```bash
rm -rf composeApp/src/commonMain/kotlin/org
```

- [ ] **Step 4: Commit — "refactor: move commonMain to com.programovil.aura package"**

---

### Task 4: Move and update iosMain files

**Files:**
- Move: `iosMain/kotlin/org/example/aura_app/Platform.ios.kt` → `iosMain/kotlin/com/programovil/aura/Platform.ios.kt`
- Move: `iosMain/kotlin/org/example/aura_app/MainViewController.kt` → `iosMain/kotlin/com/programovil/aura/MainViewController.kt`

- [ ] **Step 1: Move iosMain files**

```bash
mv composeApp/src/iosMain/kotlin/org/example/aura_app/Platform.ios.kt \
   composeApp/src/iosMain/kotlin/com/programovil/aura/Platform.ios.kt

mv composeApp/src/iosMain/kotlin/org/example/aura_app/MainViewController.kt \
   composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt
```

- [ ] **Step 2: Update package declaration in Platform.ios.kt**

In `composeApp/src/iosMain/kotlin/com/programovil/aura/Platform.ios.kt`:
- `package org.example.aura_app` → `package com.programovil.aura`

- [ ] **Step 3: Update package declaration in MainViewController.kt**

In `composeApp/src/iosMain/kotlin/com/programovil/aura/MainViewController.kt`:
- `package org.example.aura_app` → `package com.programovil.aura`

- [ ] **Step 4: Remove old iosMain directory**

```bash
rmdir composeApp/src/iosMain/kotlin/org/example/aura_app
```

- [ ] **Step 5: Commit — "refactor: move iosMain to com.programovil.aura package"**

---

### Task 5: Update OAuth client ID in MainActivity.kt

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt:42`

- [ ] **Step 1: Update the requestIdToken value**

In `composeApp/src/androidMain/kotlin/com/programovil/aura/MainActivity.kt`, line 42:
- Replace `.requestIdToken("623141386052-gpn8fq0c03i0khmt3nn9bj0h92fprnfh.apps.googleusercontent.com")`
- With `.requestIdToken("YOUR_NEW_OAUTH_CLIENT_ID.apps.googleusercontent.com")`

**⚠️ Replace `YOUR_NEW_OAUTH_CLIENT_ID` with the actual ID from Google Cloud Console** (the number before `-apps.googleusercontent.com`). You must generate this in Google Cloud Console for package `com.programovil.aura` + your debug SHA-1.

- [ ] **Step 2: Commit — "fix: update Google Sign-In client ID for com.programovil.aura"**

---

### Task 6: Verify build

**Files:**
- Test: `composeApp/build.gradle.kts` (implicit — Gradle build)

- [ ] **Step 1: Run assembleDebug to verify project builds**

```bash
./gradlew :composeApp:assembleDebug --no-daemon 2>&1
```

Expected: `BUILD SUCCESSFUL` with no errors.

If you get `package com.programovil.aura does not exist`, re-check that all package declarations were updated.

- [ ] **Step 2: Commit verification result** (only if build passes, otherwise document error and fix)

---

## Spec Coverage Check

| Spec requirement | Task |
|-----------------|------|
| Move all Kotlin sources to new directory | Tasks 2, 3, 4 |
| Update all package declarations | Tasks 2, 3, 4 |
| Update all import statements | Tasks 2, 3 |
| Update OAuth client ID | Task 5 |
| Verify build | Task 6 |
| Commit after each logical group | All tasks |

## Self-Review

- All 16 files accounted for (3 androidMain + 11 commonMain + 2 iosMain)
- No placeholder code — all steps are complete commands and exact file paths
- OAuth ID placeholder clearly marked as requiring user input
- Task 6 build verification is the functional "test" for this rename
