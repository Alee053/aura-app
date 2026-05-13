# KMP Architecture Specification

> **Document purpose:** Reference guide for the Kotlin Multiplatform (KMP) architecture used in this project.  
> **Last updated:** May 2026  
> **Scope:** Android-first KMP project targeting Android (primary) and iOS (future).

---

## Table of Contents

1. [What is KMP?](#1-what-is-kmp)
2. [Project Structure](#2-project-structure)
3. [Source Sets](#3-source-sets)
4. [The `expect`/`actual` Mechanism](#4-the-expectactual-mechanism)
5. [Dependency Declaration Per Source Set](#5-dependency-declaration-per-source-set)
6. [What Lives Where (Decision Rules)](#6-what-lives-where-decision-rules)
7. [Compilation Flow](#7-compilation-flow)
8. [Intermediate Source Sets (Hierarchical Sharing)](#8-intermediate-source-sets-hierarchical-sharing)
9. [Testing Architecture](#9-testing-architecture)
10. [Layer Architecture Inside `commonMain`](#10-layer-architecture-inside-commonmain)
11. [Common Pitfalls](#11-common-pitfalls)
12. [Recommended Multiplatform Libraries](#12-recommended-multiplatform-libraries)

---

## 1. What is KMP?

Kotlin Multiplatform (KMP) is a Kotlin compiler feature — not a framework — that lets you compile the **same Kotlin source code** into multiple platform-specific binaries. Unlike Flutter or React Native, KMP does **not** abstract the UI layer. It shares only logic (business rules, data models, networking, storage) while leaving each platform in full control of its own UI.

**What KMP is NOT:**
- It is not a cross-platform UI toolkit (use Compose Multiplatform separately if that is needed).
- It is not a virtual machine or runtime layer.
- It does not impose any UI patterns on Android or iOS.

**Core value proposition:**  
Write business logic once in `commonMain`. The Kotlin compiler emits a standard `.aar` for Android and a `.framework` for iOS — each platform consumes it natively, with zero overhead from an abstraction layer.

---

## 2. Project Structure

A standard Android-first KMP project has at minimum two Gradle modules:

```
root/
├── androidApp/              ← Android application module (UI, Activities, Composables)
│   └── build.gradle.kts
├── shared/                  ← KMP library module (all cross-platform logic)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/      ← Platform-neutral Kotlin code
│       ├── commonTest/      ← Tests for common code
│       ├── androidMain/     ← Android-only implementations
│       ├── androidUnitTest/ ← Android-specific unit tests
│       ├── iosMain/         ← iOS-only implementations (if targeting iOS)
│       └── iosTest/
├── build.gradle.kts
└── settings.gradle.kts
```

The `androidApp` module declares the `shared` module as a dependency:

```kotlin
// androidApp/build.gradle.kts
dependencies {
    implementation(project(":shared"))
}
```

There is nothing special about this dependency from Android's perspective — Gradle treats the `shared` module output as a regular `.aar` library artifact.

---

## 3. Source Sets

A **source set** is a named directory containing Kotlin source files, with its own dependency list and compiler options. The key property of a source set is the set of **targets** it compiles to.

### 3.1 Source Set Dependency Graph

Source sets form a directed acyclic graph. Platform-specific source sets depend on `commonMain` automatically — no manual `dependsOn` configuration is required for standard targets:

```
commonMain
    ├── androidMain       (compiles to: Android JVM/ART)
    └── iosMain           (compiles to: iosArm64, iosSimulatorArm64)
```

Code visibility follows this dependency direction:

- `androidMain` **can** access everything declared in `commonMain`.
- `commonMain` **cannot** access anything declared in `androidMain` or `iosMain`.

This one-way visibility is enforced by the compiler — not by convention. Attempting to use `android.*` or `java.io.*` in `commonMain` is a **compile error**.

### 3.2 Source Set Reference Table

| Source Set        | Compiles To          | Can Use                          | Typical Contents                              |
|-------------------|----------------------|----------------------------------|-----------------------------------------------|
| `commonMain`      | All declared targets | Kotlin stdlib, KMP libraries     | Models, repos, use cases, ViewModels, mappers |
| `commonTest`      | All declared targets | `kotlin.test`                    | Unit tests for common logic                   |
| `androidMain`     | Android (JVM/ART)    | Android SDK, Java libraries      | `actual` impls, Android DI config             |
| `androidUnitTest` | JVM                  | JUnit, Mockito                   | Android-specific unit tests                   |
| `iosMain`         | iOS targets          | Kotlin/Native, Foundation/UIKit  | `actual` impls for iOS                        |
| `iosTest`         | iOS targets          | `kotlin.test`, XCTest            | iOS-specific tests                            |

---

## 4. The `expect`/`actual` Mechanism

This is the primary bridge between `commonMain` and platform-specific code. When `commonMain` needs behavior that is inherently platform-specific (filesystem access, device info, cryptography, platform sensors), it declares a **contract** with `expect` and each platform fulfills it with `actual`.

### 4.1 Syntax

```kotlin
// commonMain/kotlin/platform/PlatformInfo.kt
expect class PlatformInfo() {
    val osName: String
    val sdkVersion: Int
}
```

```kotlin
// androidMain/kotlin/platform/PlatformInfo.android.kt
import android.os.Build

actual class PlatformInfo actual constructor() {
    actual val osName: String = "Android"
    actual val sdkVersion: Int = Build.VERSION.SDK_INT
}
```

```kotlin
// iosMain/kotlin/platform/PlatformInfo.ios.kt
import platform.UIKit.UIDevice

actual class PlatformInfo actual constructor() {
    actual val osName: String = UIDevice.currentDevice.systemName()
    actual val sdkVersion: Int = 0 // Retrieve from NSProcessInfo if needed
}
```

### 4.2 Enforcement

The compiler enforces **structural completeness**: every `expect` declaration in `commonMain` must have a corresponding `actual` in **every declared target**. Adding a new target without implementing all `expect` declarations results in a build failure. This is a guarantee, not a linting suggestion.

### 4.3 `expect`/`actual` Can Be Applied To

| Construct        | Notes                                                                 |
|------------------|-----------------------------------------------------------------------|
| `class`          | Full class with constructor and members                               |
| `fun` (top-level)| Standalone functions                                                  |
| `object`         | Singleton with platform-specific backing                              |
| `interface`      | Less common — prefer `expect class` or dependency injection           |
| `val` (top-level)| Constant or property backed by platform value                         |
| `typealias`      | Map a common type name to a platform-specific type                    |

### 4.4 File Naming Convention

By convention, `actual` implementations use the platform suffix in the filename:

```
PlatformInfo.kt          ← expect declaration (commonMain)
PlatformInfo.android.kt  ← actual for Android (androidMain)
PlatformInfo.ios.kt      ← actual for iOS (iosMain)
```

This is a convention enforced by tooling (Android Studio, Fleet), not the compiler.

---

## 5. Dependency Declaration Per Source Set

Dependencies are declared per source set inside the `kotlin {}` block in `shared/build.gradle.kts`. Multiplatform libraries (like Ktor, SQLDelight, kotlinx.coroutines) publish platform-specific artifacts and expect you to pick the right engine per target:

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Shared across all platforms
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
        }
        androidMain.dependencies {
            // Android-only: OkHttp engine for Ktor
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            // iOS-only: Darwin engine for Ktor
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}
```

**Key rule:** Only declare a dependency in `commonMain` if it is a proper KMP library (has a common artifact). Platform SDKs like `android.*` or `java.io.*` must go in the platform-specific source set.

---

## 6. What Lives Where (Decision Rules)

Use this decision tree when placing new code:

```
Is the code platform-neutral?
│
├─ YES → Does it depend on any platform API (android.*, java.io.*, UIKit, etc.)?
│         │
│         ├─ NO  → Place in commonMain ✓
│         └─ YES → Use expect/actual: declare in commonMain, implement in androidMain/iosMain
│
└─ NO (UI, OS-specific behavior) → Place directly in androidMain (or androidApp for UI)
```

### Concrete Placement Examples

| Code                          | Where it goes       | Reasoning                                        |
|-------------------------------|---------------------|--------------------------------------------------|
| `data class User(...)`        | `commonMain`        | Plain data, no platform APIs                     |
| `UserRepository` interface    | `commonMain`        | Interface has no platform dependency             |
| `UserRepositoryImpl` (Room)   | `androidMain`       | Room is Android-only                             |
| Ktor HTTP client setup        | `commonMain`        | Ktor core is multiplatform                       |
| Ktor OkHttp engine config     | `androidMain`       | OkHttp engine is Android-only                   |
| ViewModel (Kotlin)            | `commonMain`        | `ViewModel` from `lifecycle-viewmodel` is KMP    |
| Activity / Fragment           | `androidApp`        | Android UI — not shared                          |
| Composable functions          | `androidApp`        | Compose UI — not shared unless using CMP         |
| SQLDelight `Database` queries | `commonMain`        | SQLDelight generates KMP-compatible code         |
| `AndroidSqliteDriver`         | `androidMain`       | Driver implementation is platform-specific       |
| `expect class Logger()`       | `commonMain`        | Contract declared for cross-platform logging     |
| `actual class Logger()`       | `androidMain/iosMain` | Platform impl using `android.util.Log` / `NSLog`|

---

## 7. Compilation Flow

The Kotlin compiler processes source sets for each target independently, merging `commonMain` into every platform's compilation unit:

```
Android target:
  commonMain ──┐
               ├──► Kotlin JVM compiler ──► .aar (consumed by androidApp)
  androidMain ─┘

iOS target:
  commonMain ──┐
               ├──► Kotlin/Native compiler ──► .framework (consumed by Xcode)
  iosMain ─────┘
```

On Android, the output is standard JVM bytecode — the ART runtime on Android devices runs it with no special runtime layer. On iOS, Kotlin/Native compiles to a binary `.xcframework`, which Xcode links directly. The two compilation pipelines are completely independent; a build failure on iOS does not affect Android.

---

## 8. Intermediate Source Sets (Hierarchical Sharing)

When targeting multiple Apple platforms (iOS + macOS + watchOS + tvOS), duplicating code in each platform source set is avoided by using an **intermediate source set** that sits between `commonMain` and the platform-specific sets:

```
commonMain
    └── appleMain (intermediate — compiles to all Apple targets)
            ├── iosMain
            ├── macosMain
            └── watchosMain
```

Kotlin creates `appleMain` automatically when Apple targets are declared. Apple-specific APIs (`platform.Foundation.*`, `platform.UIKit.*`) are accessible from `appleMain` but not from `commonMain`.

For an Android-only project this is not immediately relevant, but it is the correct structure to adopt when iOS is added to the target list — avoids a painful refactor later.

---

## 9. Testing Architecture

Source sets follow a `Main`/`Test` pairing pattern. The `Test` source sets have access to their corresponding `Main` source set's API automatically:

```
commonMain ──► commonTest       (tests shared with all targets, uses kotlin.test)
androidMain ──► androidUnitTest (JVM tests, can use JUnit 4/5, Mockito)
iosMain ──► iosTest             (Kotlin/Native tests, can integrate with XCTest)
```

### Running Tests

| Task                        | What it runs                             |
|-----------------------------|------------------------------------------|
| `./gradlew :shared:allTests`| All targets' tests                       |
| `./gradlew :shared:jvmTest` | commonTest compiled to JVM               |
| `./gradlew :shared:iosArm64Test` | iOS device tests (requires macOS) |

**Recommendation:** Write the bulk of business logic tests in `commonTest` using `kotlin.test`. This gives test coverage on all targets from a single test file. Use `androidUnitTest` only for tests that require Android-specific APIs or JVM libraries.

---

## 10. Layer Architecture Inside `commonMain`

`commonMain` should follow a clean layered architecture. A typical organization:

```
commonMain/
└── kotlin/
    └── com.example.app/
        ├── data/
        │   ├── model/          ← Data classes, DTOs, database entities
        │   ├── remote/         ← API service interfaces + Ktor implementations
        │   ├── local/          ← SQLDelight or in-memory data sources
        │   └── repository/     ← Repository implementations
        ├── domain/
        │   ├── model/          ← Domain-specific models (if different from data layer)
        │   ├── repository/     ← Repository interfaces (contracts)
        │   └── usecase/        ← Use case / interactor classes
        ├── presentation/
        │   └── viewmodel/      ← Shared ViewModels (using lifecycle-viewmodel KMP)
        └── platform/
            └── *.kt            ← expect declarations for platform bridges
```

### Layer Rules

- **`domain`** has zero dependencies on `data` or `presentation`. It only knows about its own models and the repository interfaces it defines.
- **`data`** implements `domain` interfaces. It depends on `domain` but not on `presentation`.
- **`presentation`** depends on `domain` use cases. It does not import from `data` directly.
- **`platform`** contains only `expect` declarations. No business logic lives here.

---

## 11. Common Pitfalls

### 11.1 Using `java.io.*` or `java.util.*` in `commonMain`

These are JDK classes, not Kotlin stdlib. They compile fine when the only target is Android/JVM, but the moment an iOS or JS target is added, the build breaks. Use `kotlinx-io` or `kotlinx.datetime` as multiplatform alternatives.

### 11.2 Forgetting `actual` Implementations

Adding a new target to `build.gradle.kts` without providing `actual` implementations for every `expect` declaration causes a compile error. The compiler error message identifies exactly which `expect` declarations are missing their `actual` counterpart.

### 11.3 Putting Android DI Configuration in `commonMain`

Dependency injection bootstrapping (Koin modules with Android-specific drivers, Hilt component setup) belongs in `androidMain` or `androidApp`. Only the interface registrations and platform-neutral bindings belong in `commonMain`.

### 11.4 Accessing `Context` from `commonMain`

`android.content.Context` is an Android type. It cannot be used in `commonMain`. The standard solution is to inject `Context` through an `expect`/`actual` wrapper or pass it at initialization in `androidMain`.

### 11.5 Coroutines Dispatcher Assumptions

`Dispatchers.Main` requires the `kotlinx-coroutines-android` artifact on Android (declared in `androidMain.dependencies`). Do not assume `Dispatchers.Main` is available in `commonMain` without it. Always declare `kotlinx-coroutines-core` in `commonMain` and the platform-specific coroutines artifact in the platform source set.

---

## 12. Recommended Multiplatform Libraries

| Category         | Library                            | Artifact (commonMain)                          |
|------------------|------------------------------------|------------------------------------------------|
| Networking       | Ktor Client                        | `io.ktor:ktor-client-core`                     |
| Serialization    | kotlinx.serialization              | `org.jetbrains.kotlinx:kotlinx-serialization-json` |
| Async            | kotlinx.coroutines                 | `org.jetbrains.kotlinx:kotlinx-coroutines-core`|
| Local DB         | SQLDelight                         | `app.cash.sqldelight:runtime`                  |
| Date/Time        | kotlinx-datetime                   | `org.jetbrains.kotlinx:kotlinx-datetime`       |
| DI               | Koin                               | `io.insert-koin:koin-core`                     |
| ViewModel        | lifecycle-viewmodel (AndroidX KMP) | `androidx.lifecycle:lifecycle-viewmodel`       |
| Settings/Prefs   | Multiplatform Settings             | `com.russhwolf:multiplatform-settings`         |
| I/O              | kotlinx-io                         | `org.jetbrains.kotlinx:kotlinx-io-core`        |
| Logging          | Kermit                             | `co.touchlab:kermit`                           |

All libraries listed above publish proper KMP artifacts with both `commonMain` and platform-specific components. Avoid using any JVM-only library (Apache Commons, Guava, OkHttp directly) in `commonMain`.

---

*This document reflects the official Kotlin Multiplatform documentation and established community practices as of 2026. For up-to-date target declarations and Gradle DSL changes, refer to [kotlinlang.org/docs/multiplatform](https://kotlinlang.org/docs/multiplatform).*
