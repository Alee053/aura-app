# Architecture Specification

This project follows a strict Clean Architecture pattern optimized for Kotlin Multiplatform (KMP).

> For KMP-specific architecture decisions (source sets, `expect`/`actual`, compilation flow, layer rules), see [`docs/KMP_ARCHITECTURE.md`](docs/KMP_ARCHITECTURE.md).
> For implementation guides, see `docs/guides/`.

## Project Structure

```
aura-app/
├── composeApp/                     # KMP module - all shared & platform code
│   ├── src/
│   │   ├── commonMain/             # Platform-neutral Kotlin code
│   │   ├── commonTest/             # Unit tests for common logic
│   │   ├── androidMain/            # Android-specific implementations
│   │   └── iosMain/                # iOS-specific implementations
│   └── build.gradle.kts
├── designsystem/                   # Design system module (colors, typography, components)
│   ├── src/commonMain/
│   └── build.gradle.kts
├── iosApp/                         # Native iOS host app (Swift/Xcode)
├── functions/                      # Firebase Cloud Functions (TypeScript)
│   └── src/index.ts                # push notification test endpoint
├── gradle/
│   └── libs.versions.toml          # Centralized version catalog
├── docs/
│   ├── KMP_ARCHITECTURE.md         # KMP architecture reference
│   ├── TECH-STACK.md               # Full technology stack
│   └── guides/                     # Implementation guides
│       ├── KOIN_IN_KMP.md
│       ├── NAVIGATION_IN_KMP.md
│       ├── FIREBASE_IN_KMP.md
│       └── WORKMANAGER_IN_KMP.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Package Structure (`composeApp/src/commonMain/`)

```
com.programovil.aura/
├── App.kt                             # Root entry point with Bottom Navigation
├── di/
│   └── InitKoin.kt                    # Shared Koin initialization (composes all feature modules)
├── navigation/
│   ├── AppNavHost.kt                  # Shared NavHost with cross-platform routes
│   └── NavRoute.kt                    # Type-safe routes (Home, Todo, Habit, Settings)
├── auth/                              # Auth feature
│   ├── domain/AuthService.kt          # Interface for platform-specific auth
│   ├── presentation/                  # AuthViewModel, SignInScreen
│   └── di/AuthModule.kt
├── todo/                              # Todo feature - Firestore backed
│   ├── domain/                        # Models, Repository Interface, UseCases
│   ├── data/repository/               # Platform-specific Repository (expect/actual)
│   ├── presentation/                  # Screen, ViewModel, Composable
│   └── di/TodoModule.kt
├── habit/                             # Habit feature - Firestore backed (iOS stubs)
│   ├── domain/                        # Models, Repository Interface, UseCases
│   ├── data/                          # Shared Repository, Mappers
│   │   └── repository/                # Platform-specific Repository (expect/actual)
│   ├── presentation/                  # Screen, ViewModel, Composable
│   └── di/HabitModule.kt
├── home/                              # Home/Dashboard feature
│   ├── domain/                        # DashboardData model, GetDashboardDataUseCase
│   ├── presentation/                  # HomeScreen, HomeViewModel, DashboardCard
│   └── di/HomeModule.kt
├── notification/                      # Notification feature
│   ├── domain/NotificationScheduler.kt # Interface for scheduling
│   ├── data/NotificationPreferences.kt # Notification preference keys
│   ├── presentation/                  # NotificationViewModel
│   └── di/NotificationModule.kt
├── settings/                          # Settings feature
│   ├── domain/repository/ThemeRepository.kt # Theme preference interface
│   ├── data/ThemeRepositoryImpl.kt    # DataStore-backed implementation
│   ├── presentation/                  # SettingsScreen, SettingsViewModel, composables
│   └── di/SettingsModule.kt
└── shared/                            # Cross-cutting concerns
    ├── data/DataStoreFactory.kt       # expect DataStore builder
    ├── ColorUtils.kt                  # KMP Color parsing utilities
    ├── FeatureFlags.kt                # Feature flag enum definitions
    ├── FeatureFlagManager.kt          # Feature flag state management
    ├── RemoteConfigService.kt         # Interface for remote config
    └── presentation/NotificationPermissionHandler.kt # expect composable for permissions
```

## Layer Definitions

### Domain Layer (`/domain/`)
- **Models**: Pure data classes (e.g., `Habit.kt`, `Todo.kt`, `DashboardData.kt`).
- **Interfaces**: Service or Repository definitions (e.g., `AuthService.kt`, `HabitRepository.kt`).
- **Use cases**: Single-responsibility logic (e.g., `GetHabitsGroupedByDayUseCase.kt`, `GetDashboardDataUseCase.kt`).

### Data Layer (`/data/`)
- **Repository implementations**: Concrete implementations of domain interfaces via `expect`/`actual`.
- **Local Store**: DataStore KMP for preferences.
- **Remote Source**: Firebase Firestore and Firebase Remote Config (implemented via platform actuals).
- **Mappers**: DTO/Entity to Domain model transformations.

### Presentation Layer (`/presentation/`)
- **Screens**: Composables representing full navigation destinations.
- **ViewModels**: Manage UI state (shared logic in `commonMain`).
- **Composables**: Reusable UI units (e.g., `HabitItem.kt`, `TodoItem.kt`, `DashboardCard.kt`).

### Shared Layer (`/shared/`)
- Cross-cutting infrastructure: DataStore, feature flags, remote config, color utilities, notification permissions.

## Feature Flags Architecture

Feature flags are toggled via Firebase Remote Config and managed through:

```
commonMain/
├── shared/FeatureFlags.kt             # Enum: HABITS_ENABLED, TODOS_ENABLED, NOTIFICATIONS_ENABLED
├── shared/RemoteConfigService.kt      # Interface: fetchAndActivate(), getBoolean()
└── shared/FeatureFlagManager.kt       # StateFlow<Map<FeatureFlag, Boolean>>, initialize()

androidMain/
└── shared/FirebaseRemoteConfigService.kt  # Firebase Remote Config implementation

iosMain/
└── shared/StubRemoteConfigService.kt      # iOS stub (returns defaults)
```

`App.kt` reads feature flags to conditionally show/hide bottom nav tabs (e.g., the **Todos** and **Habits** tabs are toggled by `FeatureFlags.TODOS_ENABLED` and `FeatureFlags.HABITS_ENABLED`).

## DI Architecture (Per-Feature)

Features are self-contained. Their modules are composed in `di/InitKoin.kt`:

```kotlin
fun getModules(remoteConfigService: RemoteConfigService) = listOf(
    authModule,
    todoModule,
    habitModule,
    notificationModule,
    homeModule,
    settingsModule,
    module {
        single<RemoteConfigService> { remoteConfigService }
        single { FeatureFlagManager(get()) }
    }
)
```

### Koin Patterns
- **`singleOf` / `factoryOf`**: Preferred for automatic constructor injection.
- **`viewModelOf`**: For shared ViewModels.
- **`bind<Interface>()`**: To link implementations to domain interfaces.

## Persistence Patterns

### Local Persistence (Room KMP — not yet implemented)
Room KMP is listed in the tech stack but not currently used. Habit data is stored in Firestore on Android and stubbed on iOS. When local caching is added, the expected pattern is:
- `commonMain` declares `expect fun getHabitDatabaseBuilder()`.
- `androidMain` provides `AndroidSQLiteDriver`.
- `iosMain` provides `BundledSQLiteDriver`.

### Local Preferences (DataStore KMP)
Defined in `commonMain` with `expect fun createDataStore()`.
- **Settings**: Theme preference (`ThemeMode`) stored via DataStore in `settings/`.
- **Notifications**: Notification preferences stored in `notification/data/NotificationPreferences.kt`.

### Cloud Persistence (Firestore)
Abstracted behind repository interfaces to handle platform-specific SDK behavior.
- **Android**: `TodoRepositoryImpl` uses `FirebaseFirestore`.
- **iOS**: `TodoRepositoryImpl` uses Firebase iOS SDK.

## Navigation

Type-safe navigation using `androidx-navigation` with `kotlinx-serialization`.

**Routes** (in `NavRoute.kt`):
- `NavRoute.Home` — Dashboard
- `NavRoute.Todo` — Todo list
- `NavRoute.Habit` — Habit tracker
- `NavRoute.Settings` — Theme & preferences

**Pattern**:
- `@Serializable data object` for parameterless routes.
- `@Serializable data class` for routes with parameters.

## Naming Conventions
- **Files**: PascalCase.
- **Use Cases**: Verb-Noun pattern (`AddHabitUseCase`).
- **ViewModels**: `FeatureName + ViewModel`.
- **Date Handling**: Strictly use `kotlinx-datetime`.
- **UUID**: Use custom KMP-compliant random string generators.

## Testing

### Stack
- **kotlin-test**: Unit testing framework for Kotlin Multiplatform.
- **Turbine**: Flow testing library (`app.cash.turbine:turbine`).
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

### Gradle Configuration (`composeApp/build.gradle.kts`)
```kotlin
plugins {
    alias(libs.plugins.mockative)  // Applied in composeApp only (not root -- conflicts with AGP classpath)
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
./gradlew :composeApp:connectedAndroidTest      # Instrumented tests (androidInstrumentedTest)
```

### Test Structure
```
composeApp/src/
├── commonTest/kotlin/com/programovil/aura/
│   ├── shared/
│   │   └── ColorUtilsTest.kt              # Pure function tests
│   ├── todo/domain/usecase/
│   │   ├── GetTodosUseCaseTest.kt         # Turbine + Mockative mocks
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

**Test with Mockative (blocking):**
```kotlin
every { repository.getTodos() } returns flowOf(Result.success(todos))
```

**Test with Mockative (suspend):**
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

// ThemeMode follows system dark preference by default:
// if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.PURPLE
DsTheme(mode = currentThemeMode) {
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
| `primary` | Brand color -- buttons, active states, FABs |
| `accent` | Secondary accent -- gradients, highlights |
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
2. **Never** hardcode `fontSize = ...sp` -- use `AppTheme.typography.*` instead.
3. **Always** theme Material components with token overrides (e.g. `TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.surface)`).
4. **Prefer** `AppTheme.colors.primary` for interactive/accent elements and `AppTheme.colors.textSecondary` for muted content.
5. The design-system module lives at `designsystem/src/commonMain/kotlin/com/programovil/aura/designsystem/`. Tokens are defined in `theme/Color.kt`, `theme/Type.kt`, and `theme/DsTheme.kt`.

### Design System Migration Checklist (for agents)
When reviewing or migrating any screen, verify:
- [ ] **Backgrounds**: Every root screen uses `AppTheme.colors.background` or a gradient starting from it. Transparent backgrounds default to white and break the theme.
- [ ] **Loading states**: The `Loading` state composable (`CircularProgressIndicator`) must sit on a `Box` with `.background(AppTheme.colors.background)`.
- [ ] **Icons**: All `Icon` composables use an explicit `tint` token (`primary` for action icons, `textSecondary` for muted icons, `textPrimary` for standard). Never rely on default Material tints -- they do not adapt to all five palettes.
- [ ] **Buttons & actions**: Destructive or secondary actions should use `textSecondary` or `error`, not `primary`.
- [ ] **Text strings as icons**: Replace any character-based icons (e.g., "x", "+") with actual Material `Icons.*` composables.
- [ ] **Unreferenced settings**: If a toggle exists in settings but nothing in the app consumes its DataStore value, remove both the UI toggle and the preference key to avoid dead code.

### Design System Components
Reusable components are provided in the `designsystem` module under `components/`:
- `PrimaryButton` -- themed filled button (`components/button/`)
- `BasicInput` -- themed text field (`components/input/`) (no `trailingIcon`; use `OutlinedTextField` directly if an icon is needed)
- `AuraHorizontalDivider` -- themed horizontal divider (`components/divider/`)

Import pattern:
```kotlin
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.components.divider.AuraHorizontalDivider
```

## Firebase Cloud Functions

Located in `functions/` (TypeScript), used for server-side push notification dispatch:

```typescript
// functions/src/index.ts
export const sendTestNotification = functions.https.onRequest(async (req, res) => {
    await admin.messaging().send({
        notification: { title, body },
        topic: 'test-notifications'
    });
});
```

Used during development to test FCM push delivery. Not required for production builds.
