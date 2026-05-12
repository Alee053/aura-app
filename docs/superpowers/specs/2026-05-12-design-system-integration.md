# Design System Integration Spec

**Date:** 2026-05-12  
**Approach:** B — Full Integration with Clean Architecture  
**Goal:** Port the design system and new Home screens from the forked repo, merge `NotificationSettingsScreen` into a unified `SettingsScreen`, and restyle all existing screens to use the new design tokens.

---

## 1. Overview

The forked repo (`/home/ale/Downloads/aura-app/`) introduced a `designsystem` KMP module and 5 new "Inicio" screens. We will port the design system module, adapt the new screens to the current repo's Clean Architecture conventions (English naming, ViewModels, repository interfaces), and integrate them into the existing navigation graph.

The existing `NotificationSettingsScreen` will be merged into the new `SettingsScreen`, which becomes the single settings destination. The bottom navigation will change from `Todos | Habits | Notifications` to `Home | Todos | Habits`.

All screens will be wrapped in `DsTheme` and will consume `AppTheme.colors` / `AppTheme.typography` tokens.

---

## 2. Architecture

### 2.1 Design System Module
A new KMP module `:designsystem` will be created at the repo root.

```
designsystem/
├── build.gradle.kts
└── src/commonMain/kotlin/com/programovil/aura/designsystem/
    ├── theme/
    │   ├── Color.kt          # AppColors data class + 5 palettes
    │   ├── DsTheme.kt        # DsTheme composable, ThemeMode enum, AppTheme object
    │   └── Type.kt           # Typography tokens
    └── components/
        ├── button/
        │   └── PrimaryButton.kt
        ├── input/
        │   └── BasicInput.kt
        └── divider/
            └── HorizontalDivider.kt
```

- Package renamed from `com.calyrsoft.designsystem` to `com.programovil.aura.designsystem` to match repo conventions.
- `DsTheme` is a custom wrapper using `staticCompositionLocalOf`. It does **not** replace `MaterialTheme`; screens can still use Material3 composables (Button, Surface, etc.) but should reference `AppTheme.colors` for colors and `AppTheme.typography` for text styles.
- `ThemeMode` enum: `PURPLE` (default), `RED`, `GREEN`, `DARK`, `HIGH_CONTRAST`.

### 2.2 New `home` Feature
The fork's `inicio` package will be recreated as `home` following the repo's per-feature structure.

```
com.programovil.aura.home/
├── domain/
│   └── model/
│       └── HomeStats.kt              # Stubs for streak / total / average stats
├── presentation/
│   ├── viewmodel/
│   │   ├── HomeViewModel.kt          # Manages HomeScreen state (task count, stats)
│   │   ├── FocusViewModel.kt         # Manages FocusScreen timer state
│   │   ├── ProgressViewModel.kt      # Manages ProgressScreen calendar state
│   │   └── SettingsViewModel.kt      # Manages theme mode + toggles + notification prefs
│   ├── screen/
│   │   ├── HomeScreen.kt             # Dashboard with task count circle + bottom buttons
│   │   ├── TasksScreen.kt            # Task categories UI wired to TodoViewModel
│   │   ├── FocusScreen.kt            # Pomodoro-style timer UI
│   │   ├── ProgressScreen.kt         # Calendar + stats UI wired to Habit data
│   │   └── SettingsScreen.kt         # Theme selector + notification prefs + app info
│   └── composable/
│       ├── HomeButton.kt             # Reusable bottom button from HomeScreen
│       ├── CategoryChip.kt           # Reusable chip from TasksScreen
│       ├── TaskCard.kt               # Reusable card from TasksScreen
│       ├── PresetButton.kt           # Reusable timer preset from FocusScreen
│       ├── StatCard.kt               # Reusable stat card from ProgressScreen
│       ├── ThemeCard.kt              # Reusable theme selector card
│       └── PreferenceItem.kt         # Reusable toggle row
└── di/
    └── HomeModule.kt                 # Koin module for Home feature ViewModels
```

### 2.3 Theme Persistence
- `ThemeMode` will be persisted via the existing `DataStoreFactory` (shared KMP DataStore).
- A new `ThemeRepository` interface + implementation will be added under `shared/data/` or directly inside `home/data/`.
- `SettingsViewModel` will read/write `ThemeMode` to DataStore.
- `App.kt` will collect the persisted `ThemeMode` as a `State` and pass it to `DsTheme`.

---

## 3. Navigation

### 3.1 NavRoute Updates
Add new routes to `NavRoute.kt`:

```kotlin
@Serializable
data object Home : NavRoute()

@Serializable
data object Tasks : NavRoute()

@Serializable
data object Focus : NavRoute()

@Serializable
data object Progress : NavRoute()

@Serializable
data object Settings : NavRoute()
```

Remove `NotificationSettings` from `NavRoute` (merged into `Settings`).

### 3.2 Bottom Navigation
New tabs: **Home | Todos | Habits**

- **Home** tab: `NavRoute.Home`
  - The Home screen uses a full-screen gradient background. The bottom navigation bar **must be hidden** when on `NavRoute.Home` to avoid visual clash (same as fork behavior).
- **Todos** tab: `NavRoute.Todo` (existing)
- **Habits** tab: `NavRoute.Habit` (existing)

### 3.3 AppNavHost
`startDestination` changes from `NavRoute.Todo` to `NavRoute.Home`.

New destinations mapped:
- `NavRoute.Home` → `HomeScreen(...)` with callbacks to navigate to Tasks, Focus, Progress, Settings.
- `NavRoute.Tasks` → `TasksScreen(...)` wired to existing `TodoViewModel`.
- `NavRoute.Focus` → `FocusScreen(...)`
- `NavRoute.Progress` → `ProgressScreen(...)` wired to existing Habit repository/ViewModel.
- `NavRoute.Settings` → `SettingsScreen(...)` with `currentThemeMode`, `onThemeChange`, `onBackClick`.

Existing destinations remain:
- `NavRoute.Todo` → `TodoScreen(viewModel = todoViewModel, onSignOut = onSignOut)`
- `NavRoute.TodoDetail` → stub
- `NavRoute.Habit` → `HabitScreen(onSignOut = onSignOut)`

### 3.4 App.kt Changes
- Wrap the entire app in `DsTheme(mode = currentThemeMode)`.
- Inject/collect the persisted `ThemeMode` from a new `SettingsViewModel` (or directly from repository) in `App.kt`.
- Pass `currentThemeMode` and `onThemeChange` down through `AuthenticatedApp` → `AppNavHost` → `SettingsScreen`.
- Hide bottom nav when `currentDestination` is `NavRoute.Home`.
- Remove `NotificationSettings` tab from `Scaffold` bottom bar.
- Restyle `SignInScreen` to use `AppTheme.colors` / `AppTheme.typography`.

---

## 4. Screen Migration & Restyling

### 4.1 New Screens (ported from fork, renamed to English)

| Fork Screen | New Name | Package | Notes |
|-------------|----------|---------|-------|
| `InicioScreen` | `HomeScreen` | `home.presentation.screen` | Dashboard with gradient background. Navigate to Tasks/Focus/Progress/Settings. |
| `TareasScreen` | `TasksScreen` | `home.presentation.screen` | Category chips + task list. **Wire to existing `TodoViewModel`** to show real todos instead of mock data. Reuse existing `TodoItem` composable if possible, but styled with `AppTheme`. |
| `EnfoqueScreen` | `FocusScreen` | `home.presentation.screen` | Timer UI. Standalone ViewModel for timer state. Can be stub logic (countdown) for MVP. |
| `ProgresoScreen` | `ProgressScreen` | `home.presentation.screen` | Calendar + stats. **Wire to existing Habit repository** to show real streak count and active days. |
| `ConfiguracionScreen` | `SettingsScreen` | `home.presentation.screen` | Theme selector + notification toggles + app info. **Merge existing `NotificationSettingsScreen` logic** (notification scheduling toggles) into this screen. |

### 4.2 Existing Screens (restyled with design system tokens)

| Screen | Changes |
|--------|---------|
| `TodoScreen` | Replace `MaterialTheme.colorScheme.*` with `AppTheme.colors.*`. Replace `MaterialTheme.typography.*` with `AppTheme.typography.*`. Update background to gradient or `AppTheme.colors.background`. |
| `HabitScreen` | Same token replacement. Update background. |
| `NotificationSettingsScreen` | **Merged into `SettingsScreen`**. Its logic (notification scheduling preferences) moves into `SettingsViewModel` + `SettingsScreen`. The old screen file is deleted. |
| `SignInScreen` | Restyle with `AppTheme` tokens. |
| `AddHabitDialog` | Restyle input fields and buttons with `AppTheme` tokens. |
| `TodoItem` / `HabitItem` | Update colors and typography to use `AppTheme`. |

---

## 5. Gradle Changes

### 5.1 `settings.gradle.kts`
Add:
```kotlin
include(":designsystem")
```

### 5.2 `composeApp/build.gradle.kts`
In `commonMain.dependencies`:
```kotlin
implementation(project(":designsystem"))
```

### 5.3 `designsystem/build.gradle.kts`
Port from fork, adjusting:
- `namespace = "com.programovil.aura.designsystem"`
- Remove `jvm()` target if the current repo does not use it (check current `settings.gradle.kts` / `composeApp/build.gradle.kts`).

---

## 6. DI Changes

### 6.1 `HomeModule.kt`
Register new ViewModels:
```kotlin
val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::FocusViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::SettingsViewModel)
    // Theme repository
    singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
}
```

### 6.2 `InitKoin.kt`
Add `homeModule` to the modules list.

---

## 7. Data Persistence

### 7.1 Theme Mode
- Key in DataStore: `theme_mode` (string, stores enum name).
- `ThemeRepository` interface with `suspend fun getThemeMode(): ThemeMode` and `suspend fun setThemeMode(mode: ThemeMode)`.
- `SettingsViewModel` exposes `themeMode: StateFlow<ThemeMode>` and handles `onThemeChange`.

### 7.2 Notification Preferences
- Existing `NotificationSettingsScreen` likely stores prefs in DataStore or Firebase. Migrate that logic into `SettingsViewModel`.
- The `NotificationScheduler` interface remains in `notification/` domain; `SettingsViewModel` calls it.

---

## 8. Testing

- Add unit tests for `SettingsViewModel` using Turbine + Mockative (verify theme mode persistence).
- Add unit tests for `HomeViewModel` (if it holds any logic).
- Ensure existing tests still pass after package rename and `MaterialTheme` → `AppTheme` migration.

---

## 9. File Changes Summary

### New Files
- `designsystem/` — entire module (7 files)
- `home/domain/model/HomeStats.kt`
- `home/presentation/viewmodel/HomeViewModel.kt`
- `home/presentation/viewmodel/FocusViewModel.kt`
- `home/presentation/viewmodel/ProgressViewModel.kt`
- `home/presentation/viewmodel/SettingsViewModel.kt`
- `home/presentation/screen/HomeScreen.kt`
- `home/presentation/screen/TasksScreen.kt`
- `home/presentation/screen/FocusScreen.kt`
- `home/presentation/screen/ProgressScreen.kt`
- `home/presentation/screen/SettingsScreen.kt`
- `home/presentation/composable/HomeButton.kt`
- `home/presentation/composable/CategoryChip.kt`
- `home/presentation/composable/TaskCard.kt`
- `home/presentation/composable/PresetButton.kt`
- `home/presentation/composable/StatCard.kt`
- `home/presentation/composable/ThemeCard.kt`
- `home/presentation/composable/PreferenceItem.kt`
- `home/di/HomeModule.kt`
- `home/data/ThemeRepository.kt` + `ThemeRepositoryImpl.kt` (or under `shared/`)

### Modified Files
- `settings.gradle.kts`
- `composeApp/build.gradle.kts`
- `di/InitKoin.kt`
- `App.kt`
- `navigation/NavRoute.kt`
- `navigation/AppNavHost.kt`
- `todo/presentation/screen/TodoScreen.kt`
- `habit/presentation/screen/HabitScreen.kt`
- `todo/presentation/composable/TodoItem.kt`
- `habit/presentation/composable/HabitItem.kt`
- `habit/presentation/composable/AddHabitDialog.kt`

### Deleted Files
- `notification/presentation/screen/NotificationSettingsScreen.kt` (logic merged into `SettingsScreen`)

---

## 10. Success Criteria

- [ ] App builds successfully on Android and iOS.
- [ ] Bottom nav shows Home, Todos, Habits.
- [ ] Home screen displays with gradient background and hidden bottom nav.
- [ ] Navigating from Home → Tasks/Focus/Progress/Settings works.
- [ ] TasksScreen shows real todos from Firestore (via `TodoViewModel`).
- [ ] ProgressScreen shows real habit stats (streak, active days) from existing data.
- [ ] SettingsScreen allows theme switching and persists choice across app restarts.
- [ ] Notification preferences are still functional inside `SettingsScreen`.
- [ ] All existing screens (Todo, Habit, SignIn) use `AppTheme` colors/typography.
- [ ] Existing unit tests pass.
