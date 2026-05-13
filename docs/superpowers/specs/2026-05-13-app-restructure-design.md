# App Restructure & Design System Adoption Design

**Date**: 2026-05-13  
**Status**: Approved  
**Approach**: Full restructure (Approach 2) — remove broken screens, restructure feature modules per Clean Architecture, fully adopt design system

---

## 1. Problem Statement

The Aura app has accumulated several issues since the design system was added:

1. **Duplicate task screens** — `TasksScreen` (Home swipe-up) and `TodoScreen` (bottom tab) both display the same Firestore data, but TasksScreen is read-only with stubbed category filtering
2. **Focus timer is completely broken** — no countdown coroutine, no pause icon swap, skip/mute buttons are no-ops, session counter never increments
3. **Progress screen uses hardcoded data** — calendar highlights day 21 statically, productivity data is fake
4. **Design system components unused** — `PrimaryButton`, `BasicInput`, `AuraHorizontalDivider` are defined but never imported; screens hardcode `Color.White` instead of `AppTheme.colors` tokens
5. **`home/` package is a monolith** — contains 5 screens, 4 ViewModels, 7 composables, and data layer code, violating Clean Architecture per AGENTS.md
6. **Settings partially broken** — Sounds/Vibration toggles don't persist, only 3 of 5 themes exposed, duplicate `SettingsViewModel` instances
7. **Disconnected navigation** — Home screen hides bottom nav, creating an island; swipe-up is a one-way gesture, not a pager
8. **`TodoDetail` route is a dead stub**

## 2. Target Structure

### 2.1 Navigation — 4 Bottom Tabs

| Tab | Route | Screen | ViewModel |
|-----|-------|--------|-----------|
| Home | `NavRoute.Home` | `HomeScreen` (dashboard) | `HomeViewModel` |
| Todos | `NavRoute.Todo` | `TodoScreen` | `TodoViewModel` |
| Habits | `NavRoute.Habit` | `HabitScreen` | `HabitViewModel` |
| Settings | `NavRoute.Settings` | `SettingsScreen` | `SettingsViewModel` |

- Bottom nav is **always visible** (including on Home)
- No swipe-up gesture on Home — dashboard cards are tappable
- Feature flags (`TODOS_ENABLED`, `HABITS_ENABLED`) remain for conditional tab visibility

### 2.2 Removed Screens & Code

Delete the following entirely:

- `NavRoute.Tasks`, `NavRoute.Focus`, `NavRoute.Progress`, `NavRoute.TodoDetail`
- `TasksScreen`, `FocusScreen`, `ProgressScreen`
- `FocusViewModel`, `ProgressViewModel`
- `TaskCard`, `CategoryChip`, `PresetButton`, `StatCard` composables
- `HomeButton` composable (replaced by dashboard cards)

### 2.3 Feature Module Restructure

#### `home/` (dashboard only)

```
home/
├── di/HomeModule.kt
├── domain/
│   ├── model/DashboardData.kt
│   └── usecase/GetDashboardDataUseCase.kt
├── data/
│   └── (empty — data comes from repositories via use case)
└── presentation/
    ├── composable/DashboardCard.kt
    ├── screen/HomeScreen.kt
    └── viewmodel/HomeViewModel.kt
```

- `HomeViewModel` uses `GetDashboardDataUseCase` instead of directly injecting `TodoRepository`
- `GetDashboardDataUseCase` aggregates incomplete todo count + habit streak summary from their respective repositories
- `DashboardData` is a simple data class: `data class DashboardData(val incompleteTodos: Int, val completedHabitsToday: Int, val totalHabitsToday: Int, val currentStreak: Int)`

#### `settings/` (extracted from `home/`)

```
settings/
├── di/SettingsModule.kt
├── data/
│   ├── ThemeRepository.kt          # Interface (moved from home/data/)
│   └── ThemeRepositoryImpl.kt
├── domain/
│   └── (use cases not needed — simple CRUD via repository)
└── presentation/
    ├── composable/
    │   ├── ThemeCard.kt
    │   └── PreferenceItem.kt
    ├── screen/SettingsScreen.kt
    └── viewmodel/SettingsViewModel.kt
```

- `ThemeRepository` interface moves from `home/data/` to `settings/data/` (implementation stays with it)
- `SettingsModule` replaces the settings-related DI in `HomeModule`
- Single `SettingsViewModel` instance scoped at app level (remove duplicate from `AppNavHost`)

#### Unchanged modules

- `todo/` — already well-structured, no restructure needed
- `habit/` — already well-structured, no restructure needed
- `auth/` — already well-structured, no restructure needed
- `notification/` — already well-structured, no restructure needed
- `shared/` — no changes needed

### 2.4 Route Definitions

Simplified `NavRoute.kt`:

```kotlin
@Serializable
sealed class NavRoute {
    @Serializable data object Home : NavRoute()
    @Serializable data object Todo : NavRoute()
    @Serializable data object Habit : NavRoute()
    @Serializable data object Settings : NavRoute()
}
```

`AppNavHost.kt` routes reduced to 4 destinations.

## 3. Design System Adoption

### 3.1 Color Tokens

Add to `AppColors` data class in `designsystem/theme/Color.kt`:

```kotlin
data class AppColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val accent: Color,
    val isLight: Boolean,
    val error: Color,           // NEW
    val textSecondary: Color,   // NEW
)
```

All 5 palettes get the new tokens:
- `error`: `#CF6679` (standard Material dark error) for all palettes
- `textSecondary`: `Color.White.copy(alpha = 0.6f)` for all dark palettes

Replace all `Color.White` hardcodes across all screens with `AppTheme.colors.textPrimary` or `AppTheme.colors.textSecondary`.

Replace `MaterialTheme.colorScheme.error` in `App.kt` with `AppTheme.colors.error`.

### 3.2 Typography Tokens

Add to `AppTypography` in `designsystem/theme/Type.kt`:

```kotlin
data class AppTypography(
    val displayLarge: TextStyle,  // NEW — 80sp bold, 88sp line height (for big numbers)
    val headlineLarge: TextStyle,
    val headlineSmall: TextStyle,
    val titleMedium: TextStyle,   // NEW — 20sp semi-bold, 28sp line height (for card titles)
    val bodyLarge: TextStyle,    // NEW — 18sp normal, 26sp line height (for main content)
    val bodyMedium: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,    // NEW — 10sp medium, 14sp line height (for captions)
)
```

Replace all hardcoded `fontSize` overrides across screens with these tokens (e.g., `fontSize = 80.sp` → `style = AppTheme.typography.displayLarge`, `fontSize = 20.sp` overrides on `bodyMedium` → `style = AppTheme.typography.titleMedium`).

### 3.3 Component Usage

Use existing design system components in screens:

| Component | Where to use |
|-----------|-------------|
| `PrimaryButton` | Sign-in button, add-habit dialog confirm button |
| `BasicInput` | Add-todo text field, add-habit name field |
| `AuraHorizontalDivider` | Section dividers in Settings, between habit sections |
| `DashboardCard` (new) | Home screen task/habit summary cards |

Create a new `DashboardCard` composable in `designsystem/components/card/`:

```kotlin
@Composable
fun DashboardCard(
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Themed with `AppTheme.colors` — primary border, surface background, textPrimary/textSecondary text.

### 3.4 Files to Update for Color/Typography Replacement

Every file currently using `Color.White` must be updated:

- `HomeScreen.kt` (rewrite entirely)
- `SettingsScreen.kt` (rewrite section colors)
- `HabitScreen.kt`, `HabitItem.kt`, `AddHabitDialog.kt`
- `TodoScreen.kt`, `TodoItem.kt`
- `SignInScreen` (in `App.kt`)
- All remaining `home/presentation/composable/` files (those not deleted)

## 4. Settings Fixes

### 4.1 Theme Selection — All 5 Themes

Expand `SettingsScreen` to show 5 theme cards:

| Display Name | ThemeMode |
|-------------|-----------|
| Arctic Night | PURPLE |
| Forest Dawn | GREEN |
| Silent Desert | RED |
| Midnight | DARK |
| High Contrast | HIGH_CONTRAST |

### 4.2 Preference Persistence

Add `soundsEnabled` and `vibrationEnabled` to `NotificationPreferences` DataStore so they persist across app restarts. Currently they are local `remember { mutableStateOf() }` state that resets on every recomposition.

`SettingsViewModel` already observes `NotificationPreferences` flows, so wire the new preferences into the same pattern.

### 4.3 Single SettingsViewModel

Remove the duplicate `SettingsViewModel` instantiation from `AppNavHost`. Keep only the app-level instance created in `App.kt`, passing it down via parameters or using Koin's shared instance scoping.

## 5. Home Dashboard Design

### 5.1 Layout

```
┌─────────────────────────────┐
│  AURA                    ⚙️ │  ← Top bar with app name + Settings icon
│                             │
│  ┌─────────────────────┐    │
│  │  TASKS TODAY         │    │
│  │     3 remaining      │    │  ← DashboardCard, navigates to Todo tab
│  │  Tap to view →       │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │  HABIT STREAK        │    │
│  │  🔥 5 day streak    │    │  ← DashboardCard, navigates to Habit tab
│  │  3/5 done today      │    │
│  └─────────────────────┘    │
│                             │
│  ─── Home | Todos | Habits | Settings ─── │
└─────────────────────────────┘
```

- Two `DashboardCard` components using design system theming
- Data from `GetDashboardDataUseCase` which combines `TodoRepository` and `HabitRepository` data
- Settings icon in top-right corner navigates to Settings tab
- Settings icon uses `AppTheme.colors.textPrimary`
- Bottom nav always visible

### 5.2 HomeViewModel

```kotlin
class HomeViewModel(getDashboardDataUseCase: GetDashboardDataUseCase) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = ...
}

data class HomeUiState(
    val incompleteTodos: Int = 0,
    val completedHabitsToday: Int = 0,
    val totalHabitsToday: Int = 0,
    val currentStreak: Int = 0,
    val isLoading: Boolean = true
)
```

## 6. DI Changes

### `HomeModule.kt` (trimmed)

```kotlin
val homeModule = module {
    factoryOf(::GetDashboardDataUseCase)
    viewModelOf(::HomeViewModel)
}
```

Remove: `ThemeRepositoryImpl`, `FocusViewModel`, `ProgressViewModel`, `SettingsViewModel` registrations.

### `SettingsModule.kt` (new)

```kotlin
val settingsModule = module {
    singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
    viewModelOf(::SettingsViewModel)
}
```

### `InitKoin.kt`

Add `settingsModule` to the module list.

## 7. Files to Delete

| File | Reason |
|------|--------|
| `home/presentation/screen/TasksScreen.kt` | Replaced by Home dashboard → Todo tab |
| `home/presentation/screen/FocusScreen.kt` | Feature removed |
| `home/presentation/screen/ProgressScreen.kt` | Feature removed |
| `home/presentation/viewmodel/FocusViewModel.kt` | Feature removed |
| `home/presentation/viewmodel/ProgressViewModel.kt` | Feature removed |
| `home/presentation/composable/TaskCard.kt` | Only used by TasksScreen |
| `home/presentation/composable/CategoryChip.kt` | Only used by TasksScreen |
| `home/presentation/composable/PresetButton.kt` | Only used by FocusScreen |
| `home/presentation/composable/StatCard.kt` | Only used by ProgressScreen |
| `home/presentation/composable/HomeButton.kt` | Replaced by DashboardCard |

## 8. Files to Create

| File | Purpose |
|------|---------|
| `settings/di/SettingsModule.kt` | DI for settings feature |
| `settings/data/ThemeRepository.kt` | Moved from `home/data/` |
| `settings/data/ThemeRepositoryImpl.kt` | Moved from `home/data/` |
| `settings/presentation/composable/ThemeCard.kt` | Moved from `home/presentation/composable/` |
| `settings/presentation/composable/PreferenceItem.kt` | Moved from `home/presentation/composable/` |
| `settings/presentation/screen/SettingsScreen.kt` | Moved from `home/presentation/screen/` |
| `settings/presentation/viewmodel/SettingsViewModel.kt` | Moved from `home/presentation/viewmodel/` |
| `home/domain/model/DashboardData.kt` | Dashboard data models |
| `home/domain/usecase/GetDashboardDataUseCase.kt` | Dashboard data aggregation |
| `home/presentation/composable/DashboardCard.kt` | New dashboard card component |
| `designsystem/components/card/DashboardCard.kt` | Or here if design-system-level component |

## 9. Files to Modify

| File | Changes |
|------|---------|
| `navigation/NavRoute.kt` | Remove Tasks, Focus, Progress, TodoDetail routes |
| `navigation/AppNavHost.kt` | 4 routes only, remove Tasks/Focus/Progress/Settings composable blocks |
| `App.kt` | Always show bottom nav, remove duplicate SettingsViewModel, use `AppTheme.colors.error`, restructure bottom bar |
| `home/presentation/screen/HomeScreen.kt` | Complete rewrite as dashboard |
| `home/presentation/viewmodel/HomeViewModel.kt` | Use `GetDashboardDataUseCase` instead of direct `TodoRepository` |
| `home/di/HomeModule.kt` | Trimmed to HomeViewModel + use case only |
| `todo/presentation/screen/TodoScreen.kt` | Use `BasicInput`, `AppTheme.colors` tokens, new typography |
| `todo/presentation/composable/TodoItem.kt` | Use `AppTheme.colors` tokens |
| `habit/presentation/screen/HabitScreen.kt` | Use `AppTheme.colors` tokens |
| `habit/presentation/composable/HabitItem.kt` | Use `AppTheme.colors` tokens |
| `habit/presentation/composable/AddHabitDialog.kt` | Use `BasicInput`, `PrimaryButton`, `AppTheme.colors` |
| `auth/presentation/AuthViewModel.kt` | No changes needed (auth is fine) |
| `designsystem/theme/Color.kt` | Add `error` and `textSecondary` tokens to all palettes |
| `designsystem/theme/Type.kt` | Add `displayLarge`, `titleMedium`, `bodyLarge`, `labelSmall` |
| `notification/data/NotificationPreferences.kt` | Add `soundsEnabled` and `vibrationEnabled` persistence |

## 10. Out of Scope

- Light theme support (all palettes are dark — future work)
- iOS notification scheduler implementation (currently stubbed)
- `TodoDetail` screen (removed as part of simplification, can be re-added later)
- Tests for new/modified code (separate task)
- Sign-out button accessibility (currently only on TodoScreen — could be moved to Settings, but that was deferred)