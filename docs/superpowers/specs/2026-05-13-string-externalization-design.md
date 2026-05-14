# String Externalization Design

## Goal
Replace all hardcoded strings in Compose UI with `stringResource()` references to `composeResources/values/strings.xml`.

## Scope

### Already Externalized ✅
- `AddHabitDialog.kt` - uses `Res.string.*`
- `HabitScreen.kt` - uses `Res.string.*`
- `TodoScreen.kt` - uses `Res.string.*`
- `TodoItem.kt` - uses `Res.string.delete`, `Res.string.due_label`

### Files to Update ❌

**1. App.kt** - Bottom navigation labels:
- "Home" → `nav_home`
- "Todos" → `nav_todos`
- "Habits" → `nav_habits`
- "Settings" → `nav_settings`

**2. SignInScreen.kt**:
- "Aura" → `sign_in_title`
- "Sign in to sync your todos across devices" → `sign_in_subtitle`
- "Sign in with Google" → `sign_in_button`

**3. HomeScreen.kt**:
- "AURA" (branding) → `app_name` (reuse from existing)
- "TODOS TODAY" → `home_dashboard_todos_title`
- "HABIT STREAK" → `home_dashboard_habits_title`
- Dynamic subtitles: `${uiState.dashboardData.incompleteTodos} remaining` → template `home_dashboard_todos_subtitle` with `%1$d` placeholder
- `${uiState.dashboardData.completedHabitsToday}/${uiState.dashboardData.totalHabitsToday} done today` → template `home_dashboard_habits_subtitle`

**4. SettingsScreen.kt**:
- "Settings" → `settings_title` (already exists as `settings`)
- "Logout" → `logout_button`
- "Themes" → `themes_section`
- "Preferences" → `preferences_section`
- "Arctic Night", "Forest Dawn", "Silent Desert", "Midnight", "High Contrast" → use existing theme strings
- "Receive daily task reminders" → `notifications_subtitle` (already exists)
- "Reminder time" → `reminder_time_label`
- Footer: "AURA", "Version 1.0.0", "Made with love..." → already exists

**5. AddHabitDialog.kt** - Day labels:
- `dayLabels` list → `day_mon`, `day_tue`, `day_wed`, `day_thu`, `day_fri`, `day_sat`, `day_sun`

**6. HabitItem.kt**:
- Streak badge `${habitWithStatus.streak}` → `streak_format` with `%1$d` placeholder

**7. HomeScreen.kt** - Dynamic content:
- "AURA" branding → reuse `app_name_label`
- Settings icon contentDescription → `settings_content_description`

## Implementation

### Step 1: Update strings.xml
Add all missing string resources following existing naming conventions:
- Section comments for grouping
- `name="section_action"` pattern for clarity
- `%1$d`, `%1$s` placeholders for dynamic values

### Step 2: Update App.kt
```kotlin
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.nav_home
import aura_app.composeapp.generated.resources.nav_todos
// etc.
label = { Text(stringResource(Res.string.nav_home)) }
```

### Step 3: Update SignInScreen.kt
Import `stringResource` and `Res`, replace hardcoded strings.

### Step 4: Update HomeScreen.kt
Replace static strings, convert dynamic values to `stringResource(Res.string.home_dashboard_todos_subtitle, count)`.

### Step 5: Update SettingsScreen.kt
Replace hardcoded theme names with existing theme strings, add missing strings.

### Step 6: Update AddHabitDialog.kt
Replace `dayLabels` hardcoded list with `stringResource()` calls in a `remember` list.

### Step 7: Update HabitItem.kt
Replace streak `${}` with `stringResource(Res.string.streak_format, habitWithStatus.streak)`.

## Verification
- Build app to ensure all `stringResource()` calls resolve
- Check no hardcoded `"..."` text remains in UI composables