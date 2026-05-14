# String Externalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all hardcoded strings in Compose UI with `stringResource()` references to `composeResources/values/strings.xml`.

**Architecture:** Centralize all UI strings in `strings.xml` following existing naming conventions. Each composable will import generated `Res` resources and use `stringResource()` for all visible text.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Compose Resource system

---

## File Mapping

| File | Changes |
|------|---------|
| `composeApp/src/commonMain/composeResources/values/strings.xml` | Add all missing string resources |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt` | Replace nav labels with stringResource |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/screen/SignInScreen.kt` | Replace hardcoded strings |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt` | Replace hardcoded strings |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt` | Replace hardcoded strings |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt` | Replace dayLabels with stringResource |
| `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt` | Replace streak badge string |

---

## Task 1: Add strings to strings.xml

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add all missing strings**

Replace the entire contents of `strings.xml` with:

```xml
<resources>
    <!-- Navigation -->
    <string name="nav_home">Home</string>
    <string name="nav_todos">Todos</string>
    <string name="nav_habits">Habits</string>
    <string name="nav_settings">Settings</string>

    <!-- Todo Screen -->
    <string name="todos_title">My Todos</string>
    <string name="add_todo">+</string>
    <string name="new_todo_hint">New todo</string>
    <string name="empty_todos">No todos yet. Add your first one!</string>
    <string name="delete">X</string>
    <string name="sign_out">Sign Out</string>
    <string name="select_due_date">Select due date</string>
    <string name="due_date_label">Due date: %1$s</string>
    <string name="ok">OK</string>
    <string name="cancel">Cancel</string>

    <!-- Habit Screen -->
    <string name="habits_title">Habits</string>
    <string name="add_habit">Add habit</string>
    <string name="today">Today</string>
    <string name="tomorrow">Tomorrow</string>
    <string name="this_week">This Week</string>
    <string name="empty_habits">No habits yet. Tap + to add one!</string>

    <!-- Add Habit Dialog -->
    <string name="new_habit">New Habit</string>
    <string name="habit_name">Habit name</string>
    <string name="daily">Daily</string>
    <string name="weekly">Weekly</string>
    <string name="repeat_on">Repeat on:</string>
    <string name="color_label">Color:</string>
    <string name="save">Save</string>
    <string name="day_mon">M</string>
    <string name="day_tue">T</string>
    <string name="day_wed">W</string>
    <string name="day_thu">T</string>
    <string name="day_fri">F</string>
    <string name="day_sat">S</string>
    <string name="day_sun">S</string>

    <!-- Notification Settings -->
    <string name="notification_settings">Notification Settings</string>
    <string name="back">Back</string>
    <string name="daily_summary">Daily Summary</string>
    <string name="enabled">Enabled</string>
    <string name="test_notification">Test Notification</string>
    <string name="notification_time">Notification time</string>
    <string name="notification_helper">Notifications will be sent daily at the time set above.</string>
    <string name="notification_permission">Notification Permission</string>
    <string name="notification_permission_text">Allow notifications to receive daily summaries and due date reminders.</string>
    <string name="allow">Allow</string>
    <string name="deny">Deny</string>
    <string name="due_label">Due: %1$s</string>

    <!-- Settings Screen -->
    <string name="settings">Settings</string>
    <string name="settings_title">Settings</string>
    <string name="logout_button">Logout</string>
    <string name="themes">Themes</string>
    <string name="themes_section">Themes</string>
    <string name="purple_theme">Arctic Night</string>
    <string name="green_theme">Forest Dawn</string>
    <string name="red_theme">Silent Desert</string>
    <string name="dark_theme">Midnight</string>
    <string name="high_contrast_theme">High Contrast</string>
    <string name="preferences">Preferences</string>
    <string name="preferences_section">Preferences</string>
    <string name="notifications">Notifications</string>
    <string name="notifications_subtitle">Receive daily task reminders</string>
    <string name="reminder_time_label">Reminder time</string>
    <string name="sounds">Sounds</string>
    <string name="sounds_subtitle">Subtle sound effects</string>
    <string name="vibration">Vibration</string>
    <string name="vibration_subtitle">Haptic feedback</string>
    <string name="app_name_label">AURA</string>
    <string name="version">Version 1.0.0</string>
    <string name="made_with_love">Made with love for mindful productivity</string>

    <!-- Sign In Screen -->
    <string name="sign_in_title">Aura</string>
    <string name="sign_in_subtitle">Sign in to sync your todos across devices</string>
    <string name="sign_in_button">Sign in with Google</string>

    <!-- Home Screen -->
    <string name="home_dashboard_todos_title">TODOS TODAY</string>
    <string name="home_dashboard_habits_title">HABIT STREAK</string>
    <string name="home_dashboard_todos_subtitle">%1$d remaining</string>
    <string name="home_dashboard_habits_subtitle">%1$d/%2$d done today</string>
    <string name="settings_content_description">Settings</string>

    <!-- Habit Item -->
    <string name="streak_format">%1$d</string>

    <!-- Common -->
    <string name="content_description_back">Back</string>
    <string name="content_description_delete">Delete</string>
</resources>
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat: add all UI strings to strings.xml for externalization"
```

---

## Task 2: Update App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt:129-188`

- [ ] **Step 1: Add imports**

After line 45 (`import org.koin.compose.viewmodel.koinViewModel`), add:

```kotlin
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.nav_home
import aura_app.composeapp.generated.resources.nav_todos
import aura_app.composeapp.generated.resources.nav_habits
import aura_app.composeapp.generated.resources.nav_settings
import org.jetbrains.compose.resources.stringResource
```

- [ ] **Step 2: Replace nav label "Home"**

Line 131: `label = { Text("Home") }` → `label = { Text(stringResource(Res.string.nav_home)) }`

- [ ] **Step 3: Replace nav label "Todos"**

Line 146: `label = { Text("Todos") }` → `label = { Text(stringResource(Res.string.nav_todos)) }`

- [ ] **Step 4: Replace nav label "Habits"**

Line 162: `label = { Text("Habits") }` → `label = { Text(stringResource(Res.string.nav_habits)) }`

- [ ] **Step 5: Replace nav label "Settings"**

Line 177: `label = { Text("Settings") }` → `label = { Text(stringResource(Res.string.nav_settings)) }`

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/App.kt
git commit -m "feat: externalize nav bar labels to strings.xml"
```

---

## Task 3: Update SignInScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/screen/SignInScreen.kt`

- [ ] **Step 1: Add imports**

After line 24 (`import com.programovil.aura.designsystem.theme.AppTheme`), add:

```kotlin
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.sign_in_title
import aura_app.composeapp.generated.resources.sign_in_subtitle
import aura_app.composeapp.generated.resources.sign_in_button
import org.jetbrains.compose.resources.stringResource
```

- [ ] **Step 2: Replace "Aura" (line 60)**

`text = "Aura"` → `text = stringResource(Res.string.sign_in_title)`

- [ ] **Step 3: Replace sign in subtitle (line 68-69)**

`text = "Sign in to sync your todos across devices"` → `text = stringResource(Res.string.sign_in_subtitle)`

- [ ] **Step 4: Replace button text (line 78)**

`text = "Sign in with Google"` → `text = stringResource(Res.string.sign_in_button)`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/auth/presentation/screen/SignInScreen.kt
git commit -m "feat: externalize sign in screen strings"
```

---

## Task 4: Update HomeScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt`

- [ ] **Step 1: Add imports**

After line 26 (`import com.programovil.aura.home.presentation.viewmodel.HomeViewModel`), add:

```kotlin
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.app_name_label
import aura_app.composeapp.generated.resources.home_dashboard_todos_title
import aura_app.composeapp.generated.resources.home_dashboard_habits_title
import aura_app.composeapp.generated.resources.home_dashboard_todos_subtitle
import aura_app.composeapp.generated.resources.home_dashboard_habits_subtitle
import aura_app.composeapp.generated.resources.settings_content_description
import org.jetbrains.compose.resources.stringResource
```

- [ ] **Step 2: Replace "AURA" branding (line 60)**

`text = "AURA"` → `text = stringResource(Res.string.app_name_label)`

- [ ] **Step 3: Replace settings icon contentDescription (line 67)**

`contentDescription = "Settings"` → `contentDescription = stringResource(Res.string.settings_content_description)`

- [ ] **Step 4: Update DashboardCard for TODOS TODAY (lines 75-79)**

Replace:
```kotlin
DashboardCard(
    title = "TODOS TODAY",
    value = if (uiState.isLoading) "..." else uiState.dashboardData.incompleteTodos.toString(),
    subtitle = "${uiState.dashboardData.incompleteTodos} remaining",
    onClick = onTodoClick
)
```

With:
```kotlin
DashboardCard(
    title = stringResource(Res.string.home_dashboard_todos_title),
    value = if (uiState.isLoading) "..." else uiState.dashboardData.incompleteTodos.toString(),
    subtitle = if (uiState.isLoading) "" else stringResource(Res.string.home_dashboard_todos_subtitle, uiState.dashboardData.incompleteTodos),
    onClick = onTodoClick
)
```

- [ ] **Step 5: Update DashboardCard for HABIT STREAK (lines 84-89)**

Replace:
```kotlin
DashboardCard(
    title = "HABIT STREAK",
    value = if (uiState.isLoading) "..." else "${uiState.dashboardData.currentStreak}",
    subtitle = "${uiState.dashboardData.completedHabitsToday}/${uiState.dashboardData.totalHabitsToday} done today",
    onClick = onHabitClick
)
```

With:
```kotlin
DashboardCard(
    title = stringResource(Res.string.home_dashboard_habits_title),
    value = if (uiState.isLoading) "..." else uiState.dashboardData.currentStreak.toString(),
    subtitle = if (uiState.isLoading) "" else stringResource(Res.string.home_dashboard_habits_subtitle, uiState.dashboardData.completedHabitsToday, uiState.dashboardData.totalHabitsToday),
    onClick = onHabitClick
)
```

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/home/presentation/screen/HomeScreen.kt
git commit -m "feat: externalize home screen strings"
```

---

## Task 5: Update SettingsScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt`

- [ ] **Step 1: Add imports**

After line 48 (`import com.programovil.aura.shared.presentation.rememberNotificationPermissionState`), add:

```kotlin
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.settings_title
import aura_app.composeapp.generated.resources.logout_button
import aura_app.composeapp.generated.resources.themes_section
import aura_app.composeapp.generated.resources.preferences_section
import aura_app.composeapp.generated.resources.purple_theme
import aura_app.composeapp.generated.resources.green_theme
import aura_app.composeapp.generated.resources.red_theme
import aura_app.composeapp.generated.resources.dark_theme
import aura_app.composeapp.generated.resources.high_contrast_theme
import aura_app.composeapp.generated.resources.reminder_time_label
import aura_app.composeapp.generated.resources.app_name_label
import aura_app.composeapp.generated.resources.version
import aura_app.composeapp.generated.resources.made_with_love
import org.jetbrains.compose.resources.stringResource
```

- [ ] **Step 2: Replace "Settings" title (line 95)**

`text = "Settings"` → `text = stringResource(Res.string.settings_title)`

- [ ] **Step 3: Replace "Logout" text (line 110)**

`text = "Logout"` → `text = stringResource(Res.string.logout_button)`

- [ ] **Step 4: Replace "Themes" section header (line 120-121)**

`text = "Themes"` → `text = stringResource(Res.string.themes_section)`

- [ ] **Step 5: Update ThemeCard theme names (lines 127-160)**

Replace each theme name:
- `"Arctic Night"` → `stringResource(Res.string.purple_theme)`
- `"Forest Dawn"` → `stringResource(Res.string.green_theme)`
- `"Silent Desert"` → `stringResource(Res.string.red_theme)`
- `"Midnight"` → `stringResource(Res.string.dark_theme)`
- `"High Contrast"` → `stringResource(Res.string.high_contrast_theme)`

- [ ] **Step 6: Replace "Preferences" section header (line 165-166)**

`text = "Preferences"` → `text = stringResource(Res.string.preferences_section)`

- [ ] **Step 7: Replace "Reminder time" label (line 196)**

`text = "Reminder time"` → `text = stringResource(Res.string.reminder_time_label)`

- [ ] **Step 8: Replace footer "AURA" (line 283)**

`"AURA"` → `stringResource(Res.string.app_name_label)`

- [ ] **Step 9: Replace footer "Version 1.0.0" (line 288)**

`"Version 1.0.0"` → `stringResource(Res.string.version)`

- [ ] **Step 10: Replace footer "Made with love..." (line 294-295)**

`"Made with love for mindful productivity"` → `stringResource(Res.string.made_with_love)`

- [ ] **Step 11: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/settings/presentation/screen/SettingsScreen.kt
git commit -m "feat: externalize settings screen strings"
```

---

## Task 6: Update AddHabitDialog.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt`

- [ ] **Step 1: Add imports**

After line 46 (`import com.programovil.aura.shared.parseHexColor`), add:

```kotlin
import aura_app.composeapp.generated.resources.day_mon
import aura_app.composeapp.generated.resources.day_tue
import aura_app.composeapp.generated.resources.day_wed
import aura_app.composeapp.generated.resources.day_thu
import aura_app.composeapp.generated.resources.day_fri
import aura_app.composeapp.generated.resources.day_sat
import aura_app.composeapp.generated.resources.day_sun
```

- [ ] **Step 2: Replace dayLabels hardcoded list (line 53)**

Replace:
```kotlin
private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
```

With a function that returns string resources:
```kotlin
@Composable
private fun getDayLabels(): List<String> {
    return listOf(
        stringResource(Res.string.day_mon),
        stringResource(Res.string.day_tue),
        stringResource(Res.string.day_wed),
        stringResource(Res.string.day_thu),
        stringResource(Res.string.day_fri),
        stringResource(Res.string.day_sat),
        stringResource(Res.string.day_sun)
    )
}
```

- [ ] **Step 3: Update dayLabels usage (line 123)**

Replace `dayLabels.forEachIndexed` with `getDayLabels().forEachIndexed`. Since the function is composable, call it inside the composable context where `forEachIndexed` is used.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt
git commit -m "feat: externalize day labels to strings.xml"
```

---

## Task 7: Update HabitItem.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt`

- [ ] **Step 1: Add imports**

After line 22 (`import com.programovil.aura.shared.parseHexColor`), add:

```kotlin
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.streak_format
import org.jetbrains.compose.resources.stringResource
```

- [ ] **Step 2: Replace streak badge text (lines 65-69)**

Replace:
```kotlin
Text(
    text = "${habitWithStatus.streak}",
    style = AppTheme.typography.labelLarge,
    color = AppTheme.colors.primary
)
```

With:
```kotlin
Text(
    text = stringResource(Res.string.streak_format, habitWithStatus.streak),
    style = AppTheme.typography.labelLarge,
    color = AppTheme.colors.primary
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt
git commit -m "feat: externalize streak badge to strings.xml"
```

---

## Task 8: Verify Build

- [ ] **Step 1: Run Android build**

```bash
cd /home/ale/.superset/worktrees/fab315dc-71de-4f30-a3f0-0945630256b1/ale-27-refine-project-after-ds
./gradlew :composeApp:compileDebugKotlinAndroid 2>&1 | tail -50
```

Expected: BUILD SUCCESSFUL with no errors

- [ ] **Step 2: Verify no hardcoded strings remain**

Search for remaining hardcoded strings in UI files:
```bash
rg '"[A-Z][a-z].*"\s*\)' composeApp/src/commonMain --type kotlin | grep -v "Res.string" | grep -v "Icon\|imageVector\|contentDescription\|Modifier"
```

Expected: No matches in UI composable files

- [ ] **Step 3: Commit verification**

```bash
git add -A && git commit -m "chore: verify all strings externalized, build passes"
```