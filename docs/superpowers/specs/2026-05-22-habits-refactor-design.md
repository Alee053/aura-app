# Habit Feature Major Refactor — Design Spec

**Date:** 2026-05-22
**Status:** Approved
**Scope:** Habit domain model, UI (card + dialog), streak logic, data layer migration

---

## 1. Motivation

The current habit screen groups habits by day (Today, Tomorrow, This Week), uses per-day habit cards, and maps habits to specific weekdays via `daysOfWeek`. The UX is confusing because:
- A single habit appears multiple times across sections.
- Weekly habits require manually selecting which days of the week.
- The card layout tries to show too much at once.
- The create/edit dialog does not fully follow the design system (title lacks contrast, Switch uses default purple).

This refactor simplifies the mental model: **one habit = one card**, with a retroactive 7-day mini-grid and a cleaner recurrence model.

---

## 2. Goals

1. **One card per habit** in a flat list.
2. **7-day mini-grid** inside each card showing Mon–Sun (or last 7 days), with today ring-highlighted.
3. **Any day tappable** to toggle completion retroactively.
4. **Habit color** used for completed-day styling in the mini-grid.
5. **Simpler recurrence model**: Daily / Weekly (target count) / Monthly (target count).
6. **Calendar-period streaks**: consecutive periods (day/week/month) hitting the target.
7. **Minimalist period progress** shown on each card.
8. **Design-system-compliant dialog**: proper title contrast, themed Switch, recurrence selector + target count stepper.

---

## 3. Domain Model Changes

### 3.1 `Habit`

Remove `daysOfWeek`. Add `targetCount` and `MONTHLY` to `RecurrenceType`.

```kotlin
data class Habit(
    val id: String,
    val name: String,
    val recurrenceType: RecurrenceType, // DAILY | WEEKLY | MONTHLY
    val targetCount: Int,               // 1 for daily, N for weekly/monthly
    val color: String,
    val createdAt: Long
)
```

### 3.2 `RecurrenceType`

```kotlin
enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY
}
```

### 3.3 `HabitWithStatus`

Flatten to one entry per habit. Replace `weeklyCompletions` with `last7Days`.

```kotlin
data class HabitWithStatus(
    val habit: Habit,
    val currentPeriodProgress: Pair<Int, Int>, // (completed, target)
    val streak: Int,
    val last7Days: List<DayCompletion>
)
```

### 3.4 `DayCompletion`

Keep existing fields; `isScheduled` becomes always `true` since habits are no longer day-bound.

```kotlin
data class DayCompletion(
    val date: String,     // YYYY-MM-DD
    val isCompleted: Boolean,
    val isScheduled: Boolean = true
)
```

### 3.5 Removed / Deprecated

- `Habit.daysOfWeek` field → removed.
- `DaySection` enum → removed (no longer grouping by day).
- `GetHabitsGroupedByDayUseCase` → replaced by `GetHabitsWithStatusUseCase`.
- `GetHabitHistoryUseCase` → logic merged into `GetHabitsWithStatusUseCase`.

---

## 4. Streak & Period Progress Logic

### 4.1 Definitions

- **Daily**: A period = one calendar day (00:00–23:59).
- **Weekly**: A period = one calendar week, **Monday to Sunday**.
- **Monthly**: A period = one calendar month (1st to last day).

### 4.2 Streak Calculation

The streak counts **consecutive calendar periods** where the habit met its `targetCount`.

1. Start from the most recently completed period.
2. Walk backward one period at a time.
3. If a period has `completions >= targetCount`, increment streak.
4. If a period has `completions < targetCount`, stop — streak broken.
5. **The current (incomplete) period does NOT count** until it meets the target.

Examples:
- **Daily, target 1**: Last completion was yesterday, day before was completed → streak = 2.
- **Weekly (3x/week), target 3**: This week has 3 completions, last week had 3, week before had 2 → streak = 2.
- **Monthly (10x/month), target 10**: This month has 8 so far → streak = 0 (current month not counted yet).

### 4.3 Current Period Progress

Compute completions within the current calendar period:
- Daily: completions for today.
- Weekly: completions for the current Monday-Sunday week.
- Monthly: completions for the current calendar month.

Display on card as a muted ratio, e.g. `"2/3"`.

### 4.4 Last 7 Days Grid

Always show the last 7 calendar days ending at today. For each day:
- `isCompleted` = any completion exists for that date.
- Today gets a visual ring highlight (border).
- Each circle is tappable to toggle completion for that specific date.

---

## 5. UI Design

### 5.1 `HabitScreen` (Flat List)

Replace the current sectioned `LazyColumn` (Today / Tomorrow / This Week) with a flat list:

```
Scaffold
├── TopAppBar ("Habits" + today's date)
├── LazyColumn
│   ├── HabitCard (habit 1)
│   ├── HabitCard (habit 2)
│   └── ...
└── FAB (Add)
```

Empty state: same as before — centered text + "Add first habit" button.

### 5.2 `HabitCard` (Replaces `HabitItem`)

**Card container**: `Card` with `AppTheme.colors.surface`, `2.dp` elevation.

**Top row** (inside card padding):
- **Color dot**: 12dp circle filled with habit color.
- **Habit name**: `bodyMedium`, weight = 1, ellipsize if long.
- **Period progress**: `labelMedium`, `textSecondary`, e.g. `"2/3"`. Only shown for weekly/monthly. Hidden for daily (redundant with checkbox).
- **Streak count**: `labelLarge`, `primary` color. Format: just the number (e.g. `7`).
- **Today checkbox**: `Checkbox` for today's completion. Uses `AppTheme.colors.primary` for checked state.

**Bottom row** (7-day mini-grid):
- Horizontal row of 7 circles, spaced by 4dp.
- Each circle: 24dp diameter.
  - **Completed**: filled with habit color.
  - **Incomplete**: outlined with `AppTheme.colors.textSecondary` @ 30% alpha.
  - **Today**: adds a 2dp ring border in `AppTheme.colors.textPrimary`.
- Below each circle: day letter (M, T, W, T, F, S, S) in `labelSmall`, `textSecondary`.
- **Tappable**: clicking any circle toggles completion for that date.

### 5.3 `HabitDialog` (Create / Edit)

**Fixes from current dialog:**
- Title text: must use `AppTheme.colors.textPrimary` (currently missing explicit color).
- Switch/toggle: must use `AppTheme.colors.primary` for thumb and track checked states, not default Material purple.

**New layout:**

```
Dialog
├── Surface (RoundedCornerShape 16.dp, AppTheme.colors.surface)
│   ├── Column (padding 24.dp)
│   │   ├── Title: "New Habit" / "Edit Habit" (headlineSmall, textPrimary)
│   │   ├── BasicInput: habit name
│   │   ├── Recurrence selector:
│   │   │   └── Segmented control or row of chips: Daily | Weekly | Monthly
│   │   ├── Target count stepper (visible only if Weekly or Monthly):
│   │   │   └── Row: [-]  [N]  [+]
│   │   ├── Color picker: row of 6 color circles
│   │   └── Button row: Delete (if edit) | Cancel | Save
```

**Target count constraints:**
- Weekly: min 1, max 7.
- Monthly: min 1, max 31.
- Daily: fixed at 1, no stepper shown.

**Validation:** name must be non-blank.

---

## 6. Data Layer Changes

### 6.1 `HabitData` Mapper

```kotlin
data class HabitData(
    val id: String,
    val name: String,
    val recurrenceType: String,
    val targetCount: Int,
    val color: String,
    val createdAt: Long? = null
)
```

Remove `daysOfWeek` from DTO and mapper functions.

### 6.2 Firestore Schema (Android)

Update document fields from:
```
{ id, name, recurrenceType, daysOfWeek, color, createdAt }
```
To:
```
{ id, name, recurrenceType, targetCount, color, createdAt }
```

**Migration note:** Existing documents with `daysOfWeek` will be read but the field is ignored. On next save, the document is rewritten with `targetCount`.

### 6.3 iOS Stub Repository

Same changes as Android: remove `daysOfWeek` handling, add `targetCount`.

### 6.4 Use Cases

| Old | New |
|-----|-----|
| `GetHabitsGroupedByDayUseCase` | `GetHabitsWithStatusUseCase` |
| `AddHabitUseCase` | Updated signature: accept `targetCount` |
| `UpdateHabitUseCase` | Works with new `Habit` model |
| `ToggleHabitCompletionUseCase` | Unchanged (still toggles date) |
| `DeleteHabitUseCase` | Unchanged |
| `GetHabitHistoryUseCase` | **Removed** — logic merged into new use case |

### 6.5 `GetHabitsWithStatusUseCase`

```kotlin
class GetHabitsWithStatusUseCase(private val repository: HabitRepository) {
    operator fun invoke(): Flow<Result<List<HabitWithStatus>>>
}
```

For each habit:
1. Fetch all completions for that habit.
2. Compute `currentPeriodProgress` (completions in current period vs target).
3. Compute `streak` by walking backward through calendar periods.
4. Build `last7Days` by checking the last 7 dates against completions.
5. Return flat list of `HabitWithStatus`.

---

## 7. ViewModel Changes

### 7.1 `HabitListUiState`

```kotlin
data class HabitListUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
```

Remove `todayHabits`, `tomorrowHabits`, `thisWeekHabits`.

### 7.2 `HabitEvent`

```kotlin
sealed class HabitEvent {
    data class ToggleCompletion(val habitId: String, val date: String) : HabitEvent()
    data class AddHabit(
        val name: String,
        val recurrenceType: RecurrenceType,
        val targetCount: Int,
        val color: String
    ) : HabitEvent()
    data class UpdateHabit(val habit: Habit) : HabitEvent()
    data class DeleteHabit(val habitId: String) : HabitEvent()
}
```

`AddHabit` now accepts `targetCount` instead of `daysOfWeek`.

### 7.3 `HabitViewModel`

- Replace `getHabitsGroupedByDayUseCase` with `getHabitsWithStatusUseCase`.
- `loadHabits()` emits flat list into `uiState.habits`.
- `addHabit()` passes `targetCount` through.

---

## 8. String Resources

Add new keys to `strings.xml`:

```xml
<string name="recurrence_daily">Daily</string>
<string name="recurrence_weekly">Weekly</string>
<string name="recurrence_monthly">Monthly</string>
<string name="target_count_label">Times per period</string>
<string name="streak_label">streak</string>
<string name="today_highlight">Today</string>
```

Remove unused keys:
- `repeat_on`
- `day_mon` through `day_sun`
- `daily_badge`

---

## 9. Testing Plan

### 9.1 Streak Calculation Unit Tests

- Daily streak: consecutive days, gap breaks streak, today not counted until completed.
- Weekly streak: Monday-Sunday boundaries, partial week does not count, gap week breaks streak.
- Monthly streak: month boundaries, partial month does not count, gap month breaks streak.
- Edge cases: leap year month, week spanning year boundary, habit created mid-period.

### 9.2 ViewModel Tests

- `loadHabits` emits flat `List<HabitWithStatus>`.
- `ToggleCompletion` calls repository with correct habitId and date.
- `AddHabit` passes `targetCount` correctly.
- `DeleteHabit` removes habit and refreshes list.

### 9.3 Use Case Tests

- `GetHabitsWithStatusUseCase` computes correct progress, streak, and last-7-days for each habit.
- `AddHabitUseCase` creates habit with correct `targetCount`.

---

## 10. Files to Modify

### Domain
- `habit/domain/model/Habit.kt` — add `targetCount`, remove `daysOfWeek`
- `habit/domain/model/RecurrenceType.kt` — add `MONTHLY`
- `habit/domain/model/HabitWithStatus.kt` — add `currentPeriodProgress`, change `weeklyCompletions` to `last7Days`
- `habit/domain/model/DaySection.kt` — **delete**
- `habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt` — **delete**
- `habit/domain/usecase/GetHabitsWithStatusUseCase.kt` — **new**
- `habit/domain/usecase/GetHabitHistoryUseCase.kt` — **delete**
- `habit/domain/usecase/AddHabitUseCase.kt` — update signature

### Data
- `habit/data/mapper/HabitData.kt` — add `targetCount`, remove `daysOfWeek`
- `habit/data/repository/` (android + ios) — update to new model

### Presentation
- `habit/presentation/viewmodel/HabitViewModel.kt` — flat list state, updated events
- `habit/presentation/viewmodel/HabitEvent.kt` — updated (or inline in ViewModel file)
- `habit/presentation/screen/HabitScreen.kt` — flat LazyColumn
- `habit/presentation/composable/HabitItem.kt` — rewrite as HabitCard
- `habit/presentation/composable/HabitDialog.kt` — design system fixes + new recurrence UI

### DI
- `habit/di/HabitModule.kt` — register new use case, remove old ones

### Strings
- `composeResources/values/strings.xml` — add new keys, remove unused ones

### Tests
- `commonTest/kotlin/.../habit/` — update existing tests, add streak calculation tests

---

## 11. Out of Scope

- Animations on the 7-day grid (can be added later).
- Long-press on card for edit — keep existing click-to-edit behavior.
- Swipe-to-delete — not requested; use dialog delete button.
- Notification reminders per habit — separate feature.

---

## 12. Open Questions

*None remaining — design approved by user.*
