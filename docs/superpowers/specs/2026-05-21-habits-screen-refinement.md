# Habits Screen Refinement — 2026-05-21

## Status
Approved. Implementation pending.

---

## 1. Overview

Refine the Habits screen to match the Todos UX pattern and improve habit visualization. Changes span data model, use cases, repository, ViewModel, and UI layers.

**Goals:**
- Match Todos UI: FAB for creation, Card-based items, click-to-edit, empty state CTA
- Improve visualization: recurrence badge + weekly heatmap strip on each habit card
- Enable edit/delete: full HabitDialog with pre-populated fields and delete option

---

## 2. Data Model Changes

### 2.1 New Type: `DayCompletion`

```kotlin
data class DayCompletion(
    val date: String,        // YYYY-MM-DD format
    val isCompleted: Boolean,
    val isScheduled: Boolean
)
```

- `isScheduled` = true when the habit was supposed to run that day (based on `recurrenceType` and `daysOfWeek`)
- `isCompleted` = true when the user actually completed it

### 2.2 Extended `HabitWithStatus`

```kotlin
data class HabitWithStatus(
    val habit: Habit,
    val isDone: Boolean,
    val isMissed: Boolean,
    val streak: Int = 0,
    val targetDate: String = "",  // YYYY-MM-DD
    val weeklyCompletions: List<DayCompletion> = emptyList()
)
```

- `weeklyCompletions` contains exactly 7 entries: the 7 days ending on `targetDate` (inclusive, oldest-first)
- Computed in `GetHabitsGroupedByDayUseCase` — no extra DB calls needed

---

## 3. Use Case Changes

### 3.1 `GetHabitsGroupedByDayUseCase` (existing, modified)

**New logic in `groupHabitsForDate()`:**

For each `HabitWithStatus`, compute `weeklyCompletions` by looking back 7 days from `targetDate`:

```kotlin
private fun computeWeeklyCompletions(
    habit: Habit,
    completions: List<HabitCompletion>,
    targetDate: LocalDate
): List<DayCompletion> {
    val completedDates = completions
        .filter { it.habitId == habit.id }
        .map { it.completedDate }
        .toSet()

    return (0..6).map { daysAgo ->
        val date = targetDate.minus(daysAgo, DateTimeUnit.DAY)
        val dateStr = date.toString()
        val dayOfWeek = date.dayOfWeek.isoDayNumber
        val isScheduled = habit.isScheduledFor(dayOfWeek)
        val isCompleted = completedDates.contains(dateStr)
        DayCompletion(dateStr, isCompleted, isScheduled)
    }.reversed()  // oldest to newest
}
```

- No new use case needed — everything computed inline
- Uses existing `isScheduledFor()` helper already in the use case

### 3.2 New: `UpdateHabitUseCase`

```kotlin
class UpdateHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habit: Habit): Result<Unit> {
        return repository.updateHabit(habit)
    }
}
```

- Follows same pattern as `AddHabitUseCase` and `UpdateTodoUseCase`
- No new tests needed beyond wiring verification (existing `GetHabitsGroupedByDayUseCaseTest` covers the data flow)

---

## 4. Repository Changes

### 4.1 `HabitRepository` interface

Add:
```kotlin
suspend fun updateHabit(habit: Habit): Result<Unit>
```

Existing: `deleteHabit` already declared.

### 4.2 Android: `HabitRepositoryImpl`

```kotlin
override suspend fun updateHabit(habit: Habit): Result<Unit> = runCatching {
    val data = mapOf(
        "name" to habit.name,
        "recurrenceType" to habit.recurrenceType.name,
        "daysOfWeek" to habit.daysOfWeek,
        "color" to habit.color,
        "createdAt" to habit.createdAt
    )
    userHabitsCollection().document(habit.id).set(data).await()
}
```

- Same `mapOf` structure as `addHabit` — using `document(habit.id).set()` overwrites existing doc
- `deleteHabit` already implemented

### 4.3 iOS: `IosHabitRepositoryImpl`

```kotlin
override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.success(Unit)
```

- Stub returns success like other iOS stubs

---

## 5. ViewModel Changes

### 5.1 `HabitEvent`

```kotlin
sealed class HabitEvent {
    data class ToggleCompletion(val habitId: String, val date: String) : HabitEvent()
    data class AddHabit(
        val name: String,
        val recurrenceType: RecurrenceType,
        val daysOfWeek: List<Int>,
        val color: String
    ) : HabitEvent()
    data class UpdateHabit(val habit: Habit) : HabitEvent()    // NEW
    data class DeleteHabit(val habitId: String) : HabitEvent() // NEW
}
```

### 5.2 `HabitViewModel`

**Constructor** (add `UpdateHabitUseCase`):
```kotlin
class HabitViewModel(
    private val repository: HabitRepository,
    private val getHabitsGroupedByDayUseCase: GetHabitsGroupedByDayUseCase,
    private val addHabitUseCase: AddHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase
) : ViewModel()
```

**Event handler**:
```kotlin
fun onEvent(event: HabitEvent) {
    when (event) {
        is HabitEvent.ToggleCompletion -> toggleCompletion(event.habitId, event.date)
        is HabitEvent.AddHabit -> addHabit(event.name, event.recurrenceType, event.daysOfWeek, event.color)
        is HabitEvent.UpdateHabit -> updateHabit(event.habit)
        is HabitEvent.DeleteHabit -> deleteHabit(event.habitId)
    }
}

private fun updateHabit(habit: Habit) {
    viewModelScope.launch {
        updateHabitUseCase(habit)
            .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to update habit") }
    }
}

private fun deleteHabit(habitId: String) {
    viewModelScope.launch {
        repository.deleteHabit(habitId)
            .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to delete habit") }
    }
}
```

---

## 6. DI Changes (`HabitModule`)

```kotlin
val habitModule = module {
    single { createHabitRepository() }

    factoryOf(::AddHabitUseCase)
    factoryOf(::GetHabitsGroupedByDayUseCase)
    factoryOf(::UpdateHabitUseCase)        // NEW
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::GetHabitHistoryUseCase)

    viewModelOf(::HabitViewModel)
}
```

---

## 7. UI Changes

### 7.1 `HabitScreen`

**TopAppBar** — simplified:
```kotlin
TopAppBar(
    title = {
        Text(
            stringResource(Res.string.habits_title),
            style = AppTheme.typography.headlineSmall
        )
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = AppTheme.colors.surface,
        titleContentColor = AppTheme.colors.textPrimary
    )
)
```
- Remove date subtitle and `+` icon action (TopAppBar now serves only as a visual anchor)
- Remove `IconButton` `onClick` from TopAppBar entirely

**State variables** (add):
```kotlin
var editingHabit by remember { mutableStateOf<Habit?>(null) }
var showDialog by remember { mutableStateOf(false) }
```

**FAB**:
```kotlin
floatingActionButton = {
    FloatingActionButton(
        onClick = {
            editingHabit = null
            showDialog = true
        },
        containerColor = AppTheme.colors.primary
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(Res.string.add_habit),
            tint = AppTheme.colors.textPrimary
        )
    }
}
```

**HabitItem onClick**:
```kotlin
HabitItem(
    habitWithStatus = habitItem,
    onToggle = { /* existing toggle logic */ },
    onClick = {
        editingHabit = habitItem.habit
        showDialog = true
    }
)
```

**Empty state** (replace existing item):
```kotlin
if (uiState.todayHabits.isEmpty() && ...) {
    item {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(Res.string.empty_habits),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
                PrimaryButton(
                    text = stringResource(Res.string.add_first_habit),
                    onClick = {
                        editingHabit = null
                        showDialog = true
                    }
                )
            }
        }
    }
}
```

**Dialog**:
```kotlin
if (showDialog) {
    HabitDialog(
        habit = editingHabit,
        onDismiss = {
            showDialog = false
            editingHabit = null
        },
        onSave = { habit ->
            viewModel.onEvent(HabitEvent.UpdateHabit(habit))
            showDialog = false
            editingHabit = null
        },
        onDelete = editingHabit?.let { habit ->
            {
                viewModel.onEvent(HabitEvent.DeleteHabit(habit.id))
                showDialog = false
                editingHabit = null
            }
        }
    )
}
```

### 7.2 `HabitItem` (replaces existing Row-based implementation)

```kotlin
@Composable
fun HabitItem(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStatus.habit
    val isDone = habitWithStatus.isDone
    val isMissed = habitWithStatus.isMissed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Color dot + Name + Streak + Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(habit.color))
                )

                Text(
                    text = habit.name,
                    style = AppTheme.typography.bodyMedium,
                    color = when {
                        isDone -> AppTheme.colors.textPrimary.copy(alpha = 0.6f)
                        isMissed -> AppTheme.colors.primary.copy(alpha = 0.7f)
                        else -> AppTheme.colors.textPrimary
                    },
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )

                if (habitWithStatus.streak > 0) {
                    Text(
                        text = stringResource(Res.string.streak_format, habitWithStatus.streak),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.primary
                    )
                }

                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onToggle() }
                )
            }

            // Row 2: Recurrence badge
            RecurrenceBadge(habit = habit)

            // Row 3: Weekly heatmap strip
            WeeklyHeatmap(
                completions = habitWithStatus.weeklyCompletions,
                targetDate = habitWithStatus.targetDate
            )
        }
    }
}
```

**`RecurrenceBadge` composable:**
```kotlin
@Composable
private fun RecurrenceBadge(habit: Habit) {
    val label = if (habit.recurrenceType == RecurrenceType.DAILY) {
        stringResource(Res.string.daily_badge)  // "Daily"
    } else {
        // Abbreviated day labels: M T W T F S S
        habit.daysOfWeek.sorted().joinToString(" ") { day ->
            when (day) {
                1 -> "M"; 2 -> "T"; 3 -> "W"; 4 -> "T"; 5 -> "F"; 6 -> "S"; 7 -> "S"
            }
        }
    }

    Text(
        text = label,
        style = AppTheme.typography.labelLarge,
        color = AppTheme.colors.textSecondary
    )
}
```

**`WeeklyHeatmap` composable:**
```kotlin
@Composable
private fun WeeklyHeatmap(
    completions: List<DayCompletion>,
    targetDate: String
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        completions.forEachIndexed { index, day ->
            val color = when {
                day.isCompleted -> AppTheme.colors.primary
                day.isScheduled && !day.isCompleted -> AppTheme.colors.textSecondary
                else -> AppTheme.colors.textSecondary.copy(alpha = 0.3f)
            }
            val border = if (day.isScheduled && !day.isCompleted) {
                Modifier.border(1.dp, AppTheme.colors.textSecondary, CircleShape)
            } else Modifier

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (day.isCompleted) color
                        else AppTheme.colors.background
                    )
                    .then(border)
            )

            if (index < 6) {
                Text(
                    text = dayLabels[index],
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}
```

### 7.3 `HabitDialog` (renamed from `AddHabitDialog`)

**Signature change:**
```kotlin
@Composable
fun HabitDialog(
    habit: Habit?,  // null = create mode, non-null = edit mode
    onDismiss: () -> Unit,
    onSave: (habit: Habit) -> Unit,
    onDelete: (() -> Unit)? = null  // null in create mode
)
```

**Internal state initialization:**
```kotlin
var name by remember { mutableStateOf(habit?.name ?: "") }
var isDaily by remember { mutableStateOf(habit?.recurrenceType == RecurrenceType.DAILY) }
var selectedDays by remember { mutableIntStateOf(habit?.let { bitmaskFromDays(it.daysOfWeek) } ?: 0) }
var selectedColor by remember { mutableStateOf(habit?.color ?: colorPalette[0]) }
```

**Title:**
```kotlin
Text(
    text = stringResource(if (habit != null) Res.string.edit_habit else Res.string.new_habit),
    style = AppTheme.typography.headlineSmall
)
```

**Delete button** (only in edit mode):
```kotlin
if (habit != null && onDelete != null) {
    TextButton(onClick = onDelete) {
        Text(
            stringResource(Res.string.delete_habit),
            style = AppTheme.typography.labelLarge,
            color = AppTheme.colors.error
        )
    }
}
```

**onSave** builds full `Habit`:
```kotlin
PrimaryButton(
    text = stringResource(Res.string.save),
    onClick = {
        if (name.isNotBlank()) {
            val daysOfWeek = if (isDaily) emptyList()
            else (0..6).filter { (selectedDays and (1 shl it)) != 0 }.map { it + 1 }

            val habitToSave = Habit(
                id = habit?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                recurrenceType = if (isDaily) RecurrenceType.DAILY else RecurrenceType.WEEKLY,
                daysOfWeek = daysOfWeek,
                color = selectedColor,
                createdAt = habit?.createdAt ?: System.currentTimeMillis()
            )
            onSave(habitToSave)
        }
    },
    enabled = name.isNotBlank()
)
```

---

## 8. String Resources

Add to `strings.xml`:

```xml
<!-- Habit Screen -->
<string name="edit_habit">Edit Habit</string>
<string name="add_first_habit">Add first habit</string>
<string name="delete_habit">Delete</string>
<string name="daily_badge">Daily</string>
```

Also update:
```xml
<string name="empty_habits">No habits yet. Tap + to add one!</string>
```
→ Change to: `No habits yet. Add your first one!` (already correct in todos: "No todos yet. Add your first one!" — make consistent)

---

## 9. Testing

### 9.1 New tests

**`UpdateHabitUseCaseTest`**
- Verify repository.updateHabit is called with correct habit
- Verify Result.success and Result.failure paths

**`HabitViewModelTest`** — add cases:
- `updateHabit invokes use case with correct habit`
- `deleteHabit invokes repository.deleteHabit with correct id`

### 9.2 Existing tests to review

- `GetHabitsGroupedByDayUseCaseTest` (if exists) — verify weeklyCompletions structure: 7 entries, reversed order, correct date range

---

## 10. Implementation Order

1. **Data model**: Add `DayCompletion` + extend `HabitWithStatus`
2. **Use case**: Update `GetHabitsGroupedByDayUseCase` to populate `weeklyCompletions`
3. **Repository**: Add `updateHabit` to interface + Android + iOS stubs
4. **New use case**: `UpdateHabitUseCase`
5. **ViewModel**: Add events, handlers, inject `UpdateHabitUseCase`
6. **DI**: Register `UpdateHabitUseCase`
7. **UI — HabitItem**: Card wrapper, recurrence badge, weekly heatmap
8. **UI — HabitScreen**: FAB, empty state, click-to-edit wiring
9. **UI — HabitDialog**: Rename, edit mode pre-population, delete button
10. **Strings**: Add new resources
11. **Tests**: Add `UpdateHabitUseCaseTest`, extend `HabitViewModelTest`
12. **Lint/verify**: `./gradlew :composeApp:testDebugUnitTest`

---

## 11. Files Touched

| Layer | File |
|---|---|
| Domain model | `habit/domain/model/HabitWithStatus.kt` (+ new `DayCompletion.kt`) |
| Use case | `habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt` |
| Use case (new) | `habit/domain/usecase/UpdateHabitUseCase.kt` |
| Repository interface | `habit/domain/repository/HabitRepository.kt` |
| Repository Android | `androidMain/.../habit/data/repository/HabitRepositoryImpl.kt` |
| Repository iOS | `iosMain/.../habit/data/repository/HabitRepositoryImpl.kt` |
| ViewModel | `habit/presentation/viewmodel/HabitViewModel.kt` |
| DI | `habit/di/HabitModule.kt` |
| UI — item | `habit/presentation/composable/HabitItem.kt` |
| UI — dialog | `habit/presentation/composable/HabitDialog.kt` (rename from AddHabitDialog) |
| UI — screen | `habit/presentation/screen/HabitScreen.kt` |
| Strings | `commonMain/composeResources/values/strings.xml` |
| Tests | `commonTest/.../habit/.../` |