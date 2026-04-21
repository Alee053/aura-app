# Habit Tracker Design — 2026-04-21

## Context

Add a habit tracker section to the existing Kotlin Multiplatform Compose app. Currently has a todo list backed by Firestore + Firebase Auth. The habit tracker should use local persistence only (ROOM on Android, UserDefaults on iOS) with Clean Architecture following the existing patterns defined in CLAUDE.md.

## Requirements

### Habits

- **Name** — Required, free text
- **Recurrence** — Daily or Weekly (specific days of week)
- **Color** — User picks from a palette of 4-6 colors
- **Completion history** — Tracked, rolling 90-day window
- **Streak logic** — Strict: streak breaks if a scheduled day is missed

### Behavior

- Habits appear on their scheduled days
- User marks them done, they stay done (no auto-reset)
- Missed days (past unscheduled days) show as "missed" state
- Streak counting is strict (breaks on missed days)

### Data Model

```
Habit {
  id: String (UUID)
  name: String
  recurrenceType: RecurrenceType (DAILY | WEEKLY)
  daysOfWeek: List<Int> (1-7, Monday=1, Sunday=7) — empty for DAILY
  color: String (hex color code)
  createdAt: Long (timestamp)
}

HabitCompletion {
  id: String (UUID)
  habitId: String
  completedDate: String (YYYY-MM-DD format)
  completedAt: Long (timestamp)
}
```

## Architecture

### Package Structure

```
com.programovil.aura/habit/
├── data/
│   ├── repository/
│   │   └── HabitRepositoryImpl.kt  # Android: Room, iOS: UserDefaults
│   └── mapper/
│       └── HabitMapper.kt
├── domain/
│   ├── model/
│   │   ├── Habit.kt
│   │   ├── HabitCompletion.kt
│   │   ├── RecurrenceType.kt (enum)
│   │   └── HabitWithStatus.kt (UI model)
│   ├── repository/
│   │   └── HabitRepository.kt
│   └── usecase/
│       ├── GetHabitsForDayUseCase.kt
│       ├── GetHabitsGroupedByDayUseCase.kt
│       ├── AddHabitUseCase.kt
│       ├── ToggleHabitCompletionUseCase.kt
│       └── GetHabitHistoryUseCase.kt
├── presentation/
│   ├── screen/
│   │   └── HabitScreen.kt
│   ├── viewmodel/
│   │   └── HabitViewModel.kt
│   └── composable/
│       ├── HabitItem.kt
│       └── AddHabitDialog.kt
└── di/
    └── HabitModule.kt
```

### Domain Models

**Habit** — Pure data class. DAILY habits have empty `daysOfWeek`. WEEKLY habits store selected days (1-7).

**HabitCompletion** — Tracks individual completions with date string (YYYY-MM-DD) for querying.

**HabitWithStatus** — UI model combining a Habit with its completion status for a specific day:
- `isDone` — completed on that day
- `isMissed` — scheduled day, past, not completed
- `streak` — current consecutive completion count

### Use Cases

| Use Case | Input | Output |
|----------|-------|--------|
| GetHabitsGroupedByDay | date | Map&lt;DaySection, List&lt;HabitWithStatus&gt;&gt; |
| AddHabit | Habit | Result&lt;Unit&gt; |
| ToggleHabitCompletion | habitId, date, done | Result&lt;Unit&gt; |
| GetHabitHistory | habitId, fromDate, toDate | Result&lt;List&lt;HabitCompletion&gt;&gt; |

### Data Layer

**HabitRepository interface** (domain):
```kotlin
fun getHabits(): Flow<List<Habit>>
fun getCompletionsForDateRange(habitId: String, start: String, end: String): Flow<List<HabitCompletion>>
suspend fun addHabit(habit: Habit): Result<Unit>
suspend fun toggleCompletion(habitId: String, date: String): Result<Unit>
```

**Android: HabitRepositoryImpl** uses Room with two tables (habits, completions).

**iOS: HabitRepositoryImpl** uses UserDefaults with Codable serialization for habits and completions.

### DI Module (HabitModule.kt)

```kotlin
val habitModule = module {
    singleOf(::HabitRepositoryImpl).bind<HabitRepository>()

    factoryOf(::GetHabitsGroupedByDayUseCase)
    factoryOf(::AddHabitUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::GetHabitHistoryUseCase)

    viewModel { HabitViewModel(get(), get(), get(), get()) }
}
```

### Presentation Layer

**HabitViewModel**:
- `UiState`: habits grouped by day sections (Today, Tomorrow, This Week)
- `toggleCompletion(habitId, date)`: marks habit done/undone for a day
- `addHabit(name, recurrenceType, daysOfWeek, color)`: creates new habit

**HabitScreen** layout:
- Header: "Habits" title + add button
- Sections by day: "Today", "Tomorrow", "This Week"
- Each section: list of HabitItem composables
- HabitItem: color dot, name, checkbox, streak count if > 0
- Missed habits shown with muted styling

**AddHabitDialog**:
- Text field for name
- Toggle: Daily vs Weekly
- Weekly: day-of-week chip selector (M T W T F S S)
- Color palette: 6 color options
- Save / Cancel buttons

### State Management

ViewModels expose:
- `StateFlow<HabitListUiState>` — grouped habits with status
- `Events` (sealed interface) for user actions
- No Effects needed for now (no navigation triggers)

## Technical Notes

### Date grouping logic

Today: show habits where `recurrenceType == DAILY` OR `daysOfWeek` contains today's day number.

Tomorrow: same logic for tomorrow's date.

This Week: habits scheduled for days beyond tomorrow but within 7 days.

### Streak calculation

Starting from today, look backwards. Count consecutive completed scheduled days. Stop when a scheduled day is missed or no more history within 90-day window.

### Data cleanup

On app startup, delete any HabitCompletion records older than 90 days from the completion date.

## Out of Scope

- Editing existing habits (v1)
- Habit notifications/reminders
- Firestore sync or cloud backup
- Multiple habit instances per day (same habit multiple times)
- Partial weeks / custom intervals