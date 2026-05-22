# Habits Screen Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the Habits screen to match Todos UX (FAB, Card-based items, click-to-edit), add recurrence badge + weekly heatmap to each habit card, and enable edit/delete of habits.

**Architecture:** Extend `HabitWithStatus` with `weeklyCompletions` computed inline in `GetHabitsGroupedByDayUseCase`. Rename `AddHabitDialog` to `HabitDialog` with edit/delete support. Follow existing Todo patterns exactly.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Koin DI, Mockative + Turbine for tests, Firebase Firestore (Android), Kotlinx DateTime.

---

## File Structure

| File | Action | Responsibility |
|------|--------|--------------|
| `composeApp/src/commonMain/.../habit/domain/model/DayCompletion.kt` | Create | New data class for weekly heatmap entries |
| `composeApp/src/commonMain/.../habit/domain/model/HabitWithStatus.kt` | Modify | Add `weeklyCompletions` field |
| `composeApp/src/commonMain/.../habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt` | Modify | Compute `weeklyCompletions` per habit |
| `composeApp/src/commonMain/.../habit/domain/usecase/UpdateHabitUseCase.kt` | Create | New use case for updating habits |
| `composeApp/src/commonMain/.../habit/domain/repository/HabitRepository.kt` | Modify | Add `updateHabit` method |
| `composeApp/src/androidMain/.../habit/data/repository/HabitRepositoryImpl.kt` | Modify | Implement `updateHabit` |
| `composeApp/src/iosMain/.../habit/data/repository/HabitRepositoryImpl.kt` | Modify | Stub `updateHabit` |
| `composeApp/src/commonMain/.../habit/presentation/viewmodel/HabitViewModel.kt` | Modify | Add UpdateHabit + DeleteHabit events and handlers |
| `composeApp/src/commonMain/.../habit/di/HabitModule.kt` | Modify | Register `UpdateHabitUseCase` |
| `composeApp/src/commonMain/.../habit/presentation/composable/HabitItem.kt` | Modify | Card-based layout, recurrence badge, weekly heatmap |
| `composeApp/src/commonMain/.../habit/presentation/composable/HabitDialog.kt` | Create | Rename from AddHabitDialog, add edit/delete support |
| `composeApp/src/commonMain/.../habit/presentation/screen/HabitScreen.kt` | Modify | FAB, empty state, click-to-edit wiring |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | Modify | Add edit_habit, add_first_habit, delete_habit, daily_badge |
| `composeApp/src/commonTest/.../habit/domain/usecase/UpdateHabitUseCaseTest.kt` | Create | Test UpdateHabitUseCase |
| `composeApp/src/commonTest/.../habit/presentation/viewmodel/HabitViewModelTest.kt` | Modify | Add updateHabit + deleteHabit test cases |

---

### Task 1: Add `DayCompletion` model and extend `HabitWithStatus`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DayCompletion.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitWithStatus.kt`

- [ ] **Step 1: Create `DayCompletion.kt`**

```kotlin
package com.programovil.aura.habit.domain.model

data class DayCompletion(
    val date: String,
    val isCompleted: Boolean,
    val isScheduled: Boolean
)
```

- [ ] **Step 2: Extend `HabitWithStatus` with `weeklyCompletions`**

```kotlin
package com.programovil.aura.habit.domain.model

import com.programovil.aura.habit.domain.model.Habit

data class HabitWithStatus(
    val habit: Habit,
    val isDone: Boolean,
    val isMissed: Boolean,
    val streak: Int = 0,
    val targetDate: String = "",
    val weeklyCompletions: List<DayCompletion> = emptyList()
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DayCompletion.kt \
    composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitWithStatus.kt
git commit -m "feat(habit): add DayCompletion model and weeklyCompletions to HabitWithStatus"
```

---

### Task 2: Update `GetHabitsGroupedByDayUseCase` to compute `weeklyCompletions`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt`

- [ ] **Step 1: Add `computeWeeklyCompletions` helper and call it in `groupHabitsForDate`**

First read the file to see the current implementation, then edit it.

In `groupHabitsForDate`, after building the `HabitWithStatus`, add the weekly completions computation:

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
    }.reversed()
}
```

Then in `groupHabitsForDate`, add `weeklyCompletions = computeWeeklyCompletions(habit, completions, date)` to the returned `HabitWithStatus`.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL (no errors)

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt
git commit -m "feat(habit): compute weeklyCompletions for each HabitWithStatus"
```

---

### Task 3: Add `updateHabit` to `HabitRepository` interface and implementations

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`
- Modify: `composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`

- [ ] **Step 1: Add `updateHabit` to `HabitRepository` interface**

```kotlin
suspend fun updateHabit(habit: Habit): Result<Unit>
```

- [ ] **Step 2: Implement `updateHabit` in Android repository**

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

- [ ] **Step 3: Stub `updateHabit` in iOS repository**

```kotlin
override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.success(Unit)
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt \
    composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt \
    composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
git commit -m "feat(habit): add updateHabit to repository interface and implementations"
```

---

### Task 4: Create `UpdateHabitUseCase`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/UpdateHabitUseCase.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt`

- [ ] **Step 1: Create `UpdateHabitUseCase.kt`**

```kotlin
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.repository.HabitRepository

class UpdateHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habit: Habit): Result<Unit> {
        return repository.updateHabit(habit)
    }
}
```

- [ ] **Step 2: Register in `HabitModule`**

Add `factoryOf(::UpdateHabitUseCase)` to the habit module.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/UpdateHabitUseCase.kt \
    composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
git commit -m "feat(habit): add UpdateHabitUseCase and register in DI"
```

---

### Task 5: Update `HabitViewModel` with `UpdateHabit` and `DeleteHabit` events

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt`

- [ ] **Step 1: Add `UpdateHabit` and `DeleteHabit` to `HabitEvent`**

```kotlin
sealed class HabitEvent {
    data class ToggleCompletion(val habitId: String, val date: String) : HabitEvent()
    data class AddHabit(
        val name: String,
        val recurrenceType: RecurrenceType,
        val daysOfWeek: List<Int>,
        val color: String
    ) : HabitEvent()
    data class UpdateHabit(val habit: Habit) : HabitEvent()
    data class DeleteHabit(val habitId: String) : HabitEvent()
}
```

- [ ] **Step 2: Add `UpdateHabitUseCase` to constructor**

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

- [ ] **Step 3: Update `onEvent` to handle new events**

```kotlin
fun onEvent(event: HabitEvent) {
    when (event) {
        is HabitEvent.ToggleCompletion -> toggleCompletion(event.habitId, event.date)
        is HabitEvent.AddHabit -> addHabit(event.name, event.recurrenceType, event.daysOfWeek, event.color)
        is HabitEvent.UpdateHabit -> updateHabit(event.habit)
        is HabitEvent.DeleteHabit -> deleteHabit(event.habitId)
    }
}
```

- [ ] **Step 4: Add `updateHabit` and `deleteHabit` private methods**

```kotlin
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

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt
git commit -m "feat(habit): add UpdateHabit and DeleteHabit events and handlers to ViewModel"
```

---

### Task 6: Rewrite `HabitItem` with Card layout, recurrence badge, and weekly heatmap

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt`
- Note: Will need to read existing file to understand current imports

- [ ] **Step 1: Rewrite `HabitItem` to use Card, add `onClick`, add `RecurrenceBadge`, add `WeeklyHeatmap`**

The full replacement:

```kotlin
package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.streak_format
import aura_app.composeapp.generated.resources.daily_badge
import org.jetbrains.compose.resources.stringResource

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

@Composable
private fun RecurrenceBadge(habit: Habit) {
    val label = if (habit.recurrenceType == RecurrenceType.DAILY) {
        stringResource(Res.string.daily_badge)
    } else {
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

@Composable
private fun WeeklyHeatmap(
    completions: List<DayCompletion>,
    targetDate: String
) {
    if (completions.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        completions.forEachIndexed { index, day ->
            val circleColor = when {
                day.isCompleted -> AppTheme.colors.primary
                day.isScheduled && !day.isCompleted -> AppTheme.colors.background
                else -> AppTheme.colors.background
            }
            val borderModifier = if (day.isScheduled && !day.isCompleted) {
                Modifier.border(1.dp, AppTheme.colors.textSecondary, CircleShape)
            } else {
                Modifier
            }
            val alphaModifier = if (!day.isScheduled) {
                Modifier.background(AppTheme.colors.textSecondary.copy(alpha = 0.3f), CircleShape)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .then(borderModifier)
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt
git commit -m "refactor(habit): rewrite HabitItem with Card layout, recurrence badge, and weekly heatmap"
```

---

### Task 7: Rewrite `HabitDialog` (rename from `AddHabitDialog`) with edit/delete support

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitDialog.kt` (replaces AddHabitDialog)
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt`

- [ ] **Step 1: Create new `HabitDialog.kt` with edit mode support**

```kotlin
package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.components.input.BasicInput
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.cancel
import aura_app.composeapp.generated.resources.color_label
import aura_app.composeapp.generated.resources.daily
import aura_app.composeapp.generated.resources.edit_habit
import aura_app.composeapp.generated.resources.habit_name
import aura_app.composeapp.generated.resources.new_habit
import aura_app.composeapp.generated.resources.repeat_on
import aura_app.composeapp.generated.resources.save
import aura_app.composeapp.generated.resources.weekly
import aura_app.composeapp.generated.resources.day_fri
import aura_app.composeapp.generated.resources.day_mon
import aura_app.composeapp.generated.resources.day_sat
import aura_app.composeapp.generated.resources.day_sun
import aura_app.composeapp.generated.resources.day_thu
import aura_app.composeapp.generated.resources.day_tue
import aura_app.composeapp.generated.resources.day_wed
import aura_app.composeapp.generated.resources.delete_habit
import org.jetbrains.compose.resources.stringResource

private val colorPalette = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD"
)

private fun bitmaskFromDays(daysOfWeek: List<Int>): Int {
    return daysOfWeek.fold(0) { acc, day -> acc or (1 shl (day - 1)) }
}

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (habit: Habit) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var isDaily by remember { mutableStateOf(habit?.recurrenceType != RecurrenceType.WEEKLY) }
    var selectedDays by remember { mutableIntStateOf(habit?.let { bitmaskFromDays(it.daysOfWeek) } ?: 0) }
    var selectedColor by remember { mutableStateOf(habit?.color ?: colorPalette[0]) }

    val isEditMode = habit != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(if (isEditMode) Res.string.edit_habit else Res.string.new_habit),
                    style = AppTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                BasicInput(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.habit_name),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.daily),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                    Switch(
                        checked = !isDaily,
                        onCheckedChange = { isDaily = !it }
                    )
                    Text(
                        stringResource(Res.string.weekly),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary
                    )
                }

                if (!isDaily) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.repeat_on),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        getDayLabels().forEachIndexed { index, label ->
                            val dayBit = 1 shl index
                            val isSelected = (selectedDays and dayBit) != 0
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AppTheme.colors.primary
                                        else AppTheme.colors.surface.copy(alpha = 0.6f)
                                    )
                                    .clickable {
                                        selectedDays = selectedDays xor dayBit
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = AppTheme.typography.labelLarge,
                                    color = if (isSelected) AppTheme.colors.textPrimary
                                    else AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.color_label),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPalette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.border(2.dp, AppTheme.colors.textPrimary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditMode && onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text(
                                text = stringResource(Res.string.delete_habit),
                                style = AppTheme.typography.labelLarge,
                                color = AppTheme.colors.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    PrimaryButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onDismiss
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    PrimaryButton(
                        text = stringResource(Res.string.save),
                        onClick = {
                            if (name.isNotBlank()) {
                                val daysOfWeek = if (isDaily) emptyList()
                                else (0..6).filter { (selectedDays and (1 shl it)) != 0 }.map { it + 1 }

                                val habitToSave = Habit(
                                    id = habit?.id ?: java.util.UUID.randomUUID().toString(),
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
                }
            }
        }
    }
}
```

- [ ] **Step 2: Delete old `AddHabitDialog.kt`**

```bash
git rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitDialog.kt
git rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt
git commit -m "refactor(habit): rename AddHabitDialog to HabitDialog with edit/delete support"
```

---

### Task 8: Update `HabitScreen` with FAB, empty state, and click-to-edit wiring

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`

- [ ] **Step 1: Rewrite `HabitScreen` — read the file first, then apply changes**

Changes:
1. Remove `IconButton` from TopAppBar actions (keep only the title "Habits")
2. Add `floatingActionButton` with FAB
3. Add `editingHabit` and `showDialog` state variables
4. Update `HabitItem` to pass `onClick` for edit
5. Update empty state to use centered `Column` + `PrimaryButton`
6. Replace `AddHabitDialog` import + call with `HabitDialog`
7. Remove unused imports (`automirrored ExitToApp`)

Full replacement of the file:

```kotlin
package com.programovil.aura.habit.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.presentation.composable.HabitDialog
import com.programovil.aura.habit.presentation.composable.HabitItem
import com.programovil.aura.habit.presentation.viewmodel.HabitEvent
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.habits_title
import aura_app.composeapp.generated.resources.add_habit
import aura_app.composeapp.generated.resources.today
import aura_app.composeapp.generated.resources.tomorrow
import aura_app.composeapp.generated.resources.this_week
import aura_app.composeapp.generated.resources.empty_habits
import aura_app.composeapp.generated.resources.add_first_habit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
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
        },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (uiState.todayHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.today),
                            subtitle = today.toString()
                        )
                    }
                    items(uiState.todayHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            },
                            onClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }
                }

                if (uiState.tomorrowHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.tomorrow),
                            subtitle = tomorrow.toString()
                        )
                    }
                    items(uiState.tomorrowHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            },
                            onClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }
                }

                if (uiState.thisWeekHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = stringResource(Res.string.this_week),
                            subtitle = null
                        )
                    }
                    items(uiState.thisWeekHabits, key = { it.habit.id + it.targetDate }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        habitItem.targetDate
                                    )
                                )
                            },
                            onClick = {
                                editingHabit = habitItem.habit
                                showDialog = true
                            }
                        )
                    }
                }

                if (uiState.todayHabits.isEmpty() &&
                    uiState.tomorrowHabits.isEmpty() &&
                    uiState.thisWeekHabits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.empty_habits),
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
            }
        }
    }

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
}

@Composable
private fun HabitSectionHeader(title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AppTheme.typography.labelLarge,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinAndroid --quiet 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt
git commit -m "feat(habit): update HabitScreen with FAB, empty state, and click-to-edit"
```

---

### Task 9: Add new string resources

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add new strings and update `empty_habits`**

Add these strings inside `<resources>`:
```xml
<string name="edit_habit">Edit Habit</string>
<string name="add_first_habit">Add first habit</string>
<string name="delete_habit">Delete</string>
<string name="daily_badge">Daily</string>
```

Update `empty_habits` from:
`No habits yet. Tap + to add one!`
to:
`No habits yet. Add your first one!`

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(habit): add edit_habit, add_first_habit, delete_habit, daily_badge strings"
```

---

### Task 10: Add tests for `UpdateHabitUseCase` and extend `HabitViewModelTest`

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/usecase/UpdateHabitUseCaseTest.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModelTest.kt`
- Note: Read existing test files first to follow existing patterns

- [ ] **Step 1: Read existing test patterns**

```bash
cat composeApp/src/commonTest/kotlin/com/programovil/aura/todo/domain/usecase/UpdateTodoUseCaseTest.kt
cat composeApp/src/commonTest/kotlin/com/programovil/aura/todo/presentation/viewmodel/TodoViewModelTest.kt
```

- [ ] **Step 2: Create `UpdateHabitUseCaseTest.kt`**

Following the `UpdateTodoUseCaseTest` pattern:

```kotlin
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import io.mockative.classOf
import io.mockative.mock
import io.mockative.coEvery
import io.mockative.coVerify
import runTest
import kotlin.test.Test

class UpdateHabitUseCaseTest {
    private val repository = mock(classOf<HabitRepository>())
    private val useCase = UpdateHabitUseCase(repository)

    @Test
    fun `updateHabit delegates to repository`() = runTest {
        val habit = Habit(
            id = "h1",
            name = "Exercise",
            recurrenceType = RecurrenceType.DAILY,
            daysOfWeek = emptyList(),
            color = "#FF6B6B"
        )
        coEvery { repository.updateHabit(habit) } returns Result.success(Unit)

        val result = useCase(habit)

        assertTrue(result.isSuccess)
        coVerify { repository.updateHabit(habit) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `updateHabit returns failure when repository fails`() = runTest {
        val habit = Habit(
            id = "h1",
            name = "Exercise",
            recurrenceType = RecurrenceType.DAILY,
            daysOfWeek = emptyList(),
            color = "#FF6B6B"
        )
        coEvery { repository.updateHabit(habit) } returns Result.failure(Exception("DB error"))

        val result = useCase(habit)

        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 3: Read `HabitViewModelTest` and extend it**

Add test cases:
- `updateHabit invokes use case with correct habit`
- `deleteHabit invokes repository.deleteHabit with correct id`

- [ ] **Step 4: Run tests**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "*UpdateHabitUseCaseTest*" --tests "*HabitViewModelTest*" 2>&1 | tail -30`
Expected: All PASS

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/usecase/UpdateHabitUseCaseTest.kt \
    composeApp/src/commonTest/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModelTest.kt
git commit -m "test(habit): add UpdateHabitUseCaseTest and extend HabitViewModelTest"
```

---

### Task 11: Final verification

- [ ] **Step 1: Run full test suite**

Run: `./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 2: Run lint/typecheck if available**

Run: `./gradlew :composeApp:lintDebug 2>&1 | tail -20` (or equivalent)
Expected: No errors

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-21-habits-screen-refinement.md`**

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?