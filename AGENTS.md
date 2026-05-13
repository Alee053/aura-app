# Architecture Specification

This project follows a strict Clean Architecture pattern optimized for Kotlin Multiplatform (KMP), inspired by `ucbp26`.

## Package Structure

```
com.programovil.aura/
├── App.kt                          # Root entry point with Bottom Navigation
├── di/
│   └── InitKoin.kt                # Shared Koin initialization for all features
├── navigation/
│   ├── AppNavHost.kt              # Shared NavHost with cross-platform routes
│   └── NavRoute.kt                # Type-safe routes via kotlinx-serialization
├── auth/                          # Auth feature - abstracted for KMP
│   ├── domain/AuthService.kt      # Interface for platform-specific auth
│   ├── presentation/              # AuthViewModel (Shared logic)
│   └── di/AuthModule.kt
├── todo/                          # Todo feature - Firestore backed
│   ├── domain/                    # Models, Repository Interface, UseCases
│   ├── data/repository/           # Platform-specific Repository (expect/actual)
│   ├── presentation/              # Screen, ViewModel, Composable
│   └── di/TodoModule.kt
├── habit/                         # Habit feature - Room KMP backed
│   ├── domain/                    # Models, Repository Interface, UseCases
│   ├── data/                      # Shared Repository, DAOs, Entities
│   │   ├── local/HabitDatabase.kt # Shared DB definition & expect builder
│   │   └── repository/            # Shared Repository implementation
│   ├── presentation/              # Screen, ViewModel, Composable
│   └── di/HabitModule.kt
├── notification/                  # Notification feature
│   ├── domain/NotificationScheduler.kt # Interface for scheduling
│   ├── presentation/              # ViewModel, Settings Screen
│   └── di/NotificationModule.kt
└── shared/                        # Cross-cutting concerns
    ├── data/DataStoreFactory.kt   # Shared KMP DataStore
    └── ColorUtils.kt              # KMP Color parsing utilities
```

## Layer Definitions

### Domain Layer (`/domain/`)
- **Models**: Pure data classes (e.g., `Habit.kt`).
- **Interfaces**: Service or Repository definitions (e.g., `AuthService.kt`, `HabitRepository.kt`).
- **Use cases**: Single-responsibility logic (e.g., `GetHabitsGroupedByDayUseCase.kt`).

### Data Layer (`/data/`)
- **Repository implementations**: Concrete implementations of domain interfaces.
- **Local Source**: Room KMP (`HabitDatabase`), platform `actual` builders.
- **Remote Source**: Firebase Firestore (implemented via platform actuals).
- **Mappers**: DTO/Entity to Domain model transformations.

### Presentation Layer (`/presentation/`)
- **Screens**: Composables representing full navigation destinations.
- **ViewModels**: Manage UI state (Shared logic).
- **Composables**: Reusable UI units (e.g., `HabitItem.kt`).

## DI Architecture (Per-Feature)

Features are self-contained. Their modules are composed in `di/InitKoin.kt`.

### Koin Patterns
- **`singleOf` / `factoryOf`**: Preferred for automatic constructor injection.
- **`viewModelOf`**: For shared ViewModels.
- **`bind<Interface>()`**: To link implementations to domain interfaces.

## Persistence Patterns

### Local Persistence (Room KMP)
Defined in `commonMain` with `expect fun getHabitDatabaseBuilder()`.
- **Android**: Stores in app database path using `AndroidSQLiteDriver`.
- **iOS**: Stores in `NSHomeDirectory` using `BundledSQLiteDriver`.

### Cloud Persistence (Firestore)
Abstracted behind repository interfaces to handle platform-specific SDK behavior.

## Naming Conventions
- **Files**: PascalCase.
- **Use Cases**: Verb-Noun pattern (`AddHabitUseCase`).
- **ViewModels**: `FeatureName + ViewModel`.
- **Date Handling**: Strictly use `kotlinx-datetime`.
- **UUID**: Use custom KMP-compliant random string generators.

## Navigation
Type-safe navigation using `androidx-navigation`.
- **Object Routes**: For simple destinations.
- **Data Class Routes**: For destinations with parameters.

## Testing

### Stack
- **kotlin-test**: Unit testing framework for Kotlin Multiplatform.
- **Turbine**: Flow testing library for coroutines (`app.cash.turbine:turbine`).
- **Mockative**: Mocking library with KSP code generation (`io.mockative:mockative`).
- **compose-ui-test-junit4**: Android instrumented tests for Compose UI.

### Dependencies (libs.versions.toml)
```toml
[versions]
coroutines = "1.8.1"
turbine = "1.1.0"
mockative = "3.2.3"

[libraries]
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockative = { module = "io.mockative:mockative", version.ref = "mockative" }
compose-ui-test-junit4 = { module = "org.jetbrains.compose.ui:ui-test-junit4", version.ref = "composeMultiplatform" }

[plugins]
mockative = { id = "io.mockative", version.ref = "mockative" }
```

### Gradle Configuration (composeApp/build.gradle.kts)
```kotlin
plugins {
    alias(libs.plugins.mockative)  // Applied in composeApp only (not root — conflicts with AGP classpath)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.mockative)  // Required for @Mockable annotation
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mockative)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
        }
    }
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
```

### gradle.properties (required for mockative)
```properties
# KSP2 required by Mockative
ksp.useKSP2=true
ksp.incremental=false
# Configuration cache disabled — Mockative plugin incompatible with it
org.gradle.configuration-cache=false
```

### @Mockable annotation
Apply `@Mockable` to interfaces or classes that need to be mocked by Mockative:
```kotlin
@Mockable
interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
}
```

### Running tests
```bash
./gradlew :composeApp:testDebugUnitTest         # Android unit tests (commonTest)
./gradlew :composeApp:testDebugUnitTest        # iOS tests via multiplatform plugin
./gradlew :composeApp:connectedAndroidTest     # Instrumented tests (androidInstrumentedTest)
```

### Test Structure
```
composeApp/src/
├── commonTest/kotlin/com/programovil/aura/
│   ├── shared/
│   │   └── ColorUtilsTest.kt              # Pure function tests
│   ├── todo/domain/usecase/
│   │   ├── GetTodosUseCaseTest.kt         # Turbine + Mokative mocks
│   │   └── AddTodoUseCaseTest.kt          # coEvery + coVerify
│   └── habit/domain/model/
│       ├── RecurrenceTypeTest.kt          # Enum tests
│       └── DaySectionTest.kt             # Enum tests
```

### Pattern examples

**Test with Turbine (Flow):**
```kotlin
useCase().test {
    val result = awaitItem()
    assertTrue(result.isSuccess)
    awaitComplete()
}
```

**Test with Mokative (blocking):**
```kotlin
every { repository.getTodos() } returns flowOf(Result.success(todos))
```

**Test with Mokative (suspend):**
```kotlin
coEvery { repository.addTodo("Buy milk", null) } returns Result.success(Unit)
coVerify { repository.addTodo("Buy milk", null) }.wasInvoked(exactly = 1)
```

### Note
The `mockative` plugin is only applied in `composeApp/build.gradle.kts`, not in the root `build.gradle.kts`. Adding it at root level causes classpath conflicts with the Android Gradle Plugin (AGP).

---

## Design System

The app uses a custom design-system module (`designsystem/`) that provides theme tokens via `CompositionLocal`. All UI code in `composeApp/` must consume these tokens instead of hardcoded values or `MaterialTheme.colorScheme`.

### Entry Point
Wrap the app (or a screen) with `DsTheme`:
```kotlin
import com.programovil.aura.designsystem.theme.DsTheme
import com.programovil.aura.designsystem.theme.ThemeMode

DsTheme(mode = ThemeMode.PURPLE) {
    // composables here
}
```

### Accessing Tokens
Use the `AppTheme` object inside any `@Composable`:
```kotlin
import com.programovil.aura.designsystem.theme.AppTheme

Text(
    text = "Hello",
    style = AppTheme.typography.bodyMedium,
    color = AppTheme.colors.textPrimary
)
```

### Color Tokens (`AppColors`)
| Token | Role |
|-------|------|
| `primary` | Brand color — buttons, active states, FABs |
| `accent` | Secondary accent — gradients, highlights |
| `background` | Root screen background |
| `surface` | Cards, dialogs, bottom sheets, nav bar |
| `textPrimary` | Main text, icons |
| `textSecondary` | Subtitles, hints, disabled text (default = `textPrimary` @ 60%) |
| `error` | Validation errors, destructive actions (default = `#CF6679`) |
| `isLight` | Palette metadata for adaptive decisions |

### Typography Tokens (`AppTypography`)
| Token | Size / Weight | Usage |
|-------|---------------|-------|
| `displayLarge` | 80sp / Light | Dashboard KPIs, large numbers |
| `headlineLarge` | 32sp / Bold | Screen titles, "AURA" branding |
| `headlineSmall` | 24sp / SemiBold | Section headers, top-app-bar titles |
| `titleMedium` | 20sp / SemiBold | Card titles, section labels |
| `bodyLarge` | 18sp / Normal | Primary readable body text |
| `bodyMedium` | 16sp / Normal | Default body text, list items |
| `labelLarge` | 14sp / Medium | Buttons, captions, metadata |
| `labelMedium` | 12sp / Medium | Timestamps, small labels |
| `labelSmall` | 10sp / Medium | Version strings, footers |

### Available Palettes
| `ThemeMode` | Name | Mood |
|-------------|------|------|
| `PURPLE` | Arctic Night | Default dark purple |
| `GREEN` | Forest Dawn | Calm green |
| `RED` | Silent Desert | Warm red/brown |
| `DARK` | Midnight | Neutral dark |
| `HIGH_CONTRAST` | High Contrast | Accessibility black/yellow |

### Rules
1. **Never** hardcode `Color.White`, `Color.Black`, or `MaterialTheme.colorScheme` in `composeApp/`.
2. **Never** hardcode `fontSize = …sp` — use `AppTheme.typography.*` instead.
3. **Always** theme Material components with token overrides (e.g. `TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.surface)`).
4. **Prefer** `AppTheme.colors.primary` for interactive/accent elements and `AppTheme.colors.textSecondary` for muted content.
5. The design-system module lives at `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/`. Tokens are defined in `theme/Color.kt`, `theme/Type.kt`, and `theme/DsTheme.kt`.

### Design System Components
Reusable components are also provided in the `designsystem` module under `components/`:
- `PrimaryButton` — themed filled button
- `BasicInput` — themed text field (no `trailingIcon`; use `OutlinedTextField` directly if an icon is needed)

Import pattern:
```kotlin
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
```
