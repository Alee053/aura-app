# Habit Major Refactor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the habit feature: flat card-per-habit UI with 7-day mini-grid, new recurrence model (daily/weekly N×/monthly N×), calendar-period streak logic, and design-system-compliant dialog.

**Architecture:** Clean Architecture with domain/data/presentation layers. KMP module at `composeApp/`. Platform-specific Firestore repo (Android) and stub (iOS).

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Koin DI, kotlinx-datetime, Firebase Firestore.

---

## Task 1: Domain Model Updates

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/RecurrenceType.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/Habit.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitWithStatus.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DayCompletion.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DaySection.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitData.kt`

### 1.1: Update `RecurrenceType` — add `MONTHLY`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/RecurrenceType.kt
package com.programovil.aura.habit.domain.model

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY
}
```

### 1.2: Update `Habit` — add `targetCount`, remove `daysOfWeek`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/Habit.kt
package com.programovil.aura.habit.domain.model

data class Habit(
    val id: String,
    val name: String,
    val recurrenceType: RecurrenceType,
    val targetCount: Int,  // 1 for daily, N for weekly/monthly
    val color: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 1.3: Update `HabitWithStatus` — flatten, add progress + last7Days

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitWithStatus.kt
package com.programovil.aura.habit.domain.model

data class HabitWithStatus(
    val habit: Habit,
    val currentPeriodProgress: Pair<Int, Int>,  // (completed, target)
    val streak: Int,
    val last7Days: List<DayCompletion>
)
```

### 1.4: Update `DayCompletion` — keep fields, `isScheduled` always true

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DayCompletion.kt
package com.programovil.aura.habit.domain.model

data class DayCompletion(
    val date: String,  // YYYY-MM-DD
    val isCompleted: Boolean,
    val isScheduled: Boolean = true  // always true in new model
)
```

### 1.5: Delete `DaySection.kt`

Delete the file entirely (no more TODAY/TOMORROW/THIS_WEEK grouping).

### 1.6: Update `HabitData` mapper — remove `daysOfWeek`, add `targetCount`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitData.kt
package com.programovil.aura.habit.data.mapper

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType

data class HabitData(
    val id: String,
    val name: String,
    val recurrenceType: String,
    val targetCount: Int,
    val color: String,
    val createdAt: Long? = null
)

data class HabitCompletionData(
    val id: String,
    val habitId: String,
    val completedDate: String,
    val completedAt: Long? = null
)

fun HabitData.toDomain(): Habit = Habit(
    id = id,
    name = name,
    recurrenceType = RecurrenceType.valueOf(recurrenceType),
    targetCount = targetCount,
    color = color,
    createdAt = createdAt ?: 0L
)

fun Habit.toData(): HabitData = HabitData(
    id = id,
    name = name,
    recurrenceType = recurrenceType.name,
    targetCount = targetCount,
    color = color,
    createdAt = createdAt
)

fun HabitCompletionData.toDomain(): HabitCompletion = HabitCompletion(
    id = id,
    habitId = habitId,
    completedDate = completedDate,
    completedAt = completedAt ?: 0L
)

fun HabitCompletion.toData(): HabitCompletionData = HabitCompletionData(
    id = id,
    habitId = habitId,
    completedDate = completedDate,
    completedAt = completedAt
)
```

---

## Task 2: Write `GetHabitsWithStatusUseCase` (Core Streak Logic)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsWithStatusUseCase.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitHistoryUseCase.kt`

### 2.1: Write the new use case with full streak logic

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsWithStatusUseCase.kt
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.*

class GetHabitsWithStatusUseCase(private val repository: HabitRepository) {

    operator fun invoke(): Flow<Result<List<HabitWithStatus>>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return combine(
            repository.getHabits(),
            repository.getAllCompletions()
        ) { habitsResult, completionsResult ->
            habitsResult.fold(
                onSuccess = { habits ->
                    completionsResult.fold(
                        onSuccess = { completions ->
                            val result = habits.map { habit ->
                                val habitCompletions = completions.filter { it.habitId == habit.id }
                                val streak = calculateStreak(habit, habitCompletions, today)
                                val progress = currentPeriodProgress(habit, habitCompletions.map { it.completedDate }, today)
                                val last7 = computeLast7Days(habitCompletions.map { it.completedDate }, today)
                                HabitWithStatus(
                                    habit = habit,
                                    currentPeriodProgress = progress,
                                    streak = streak,
                                    last7Days = last7
                                )
                            }
                            Result.success(result)
                        },
                        onFailure = { Result.failure(it) }
                    )
                },
                onFailure = { Result.failure(it) }
            )
        }
    }

    private data class Period(val startDate: LocalDate, val endDate: LocalDate)

    private fun getPeriodContaining(date: LocalDate, type: RecurrenceType): Period {
        return when (type) {
            RecurrenceType.DAILY -> Period(date, date)
            RecurrenceType.WEEKLY -> {
                val daysSinceMonday = (date.dayOfWeek.isoDayNumber - 1) % 7
                val monday = date.minus(daysSinceMonday, DateTimeUnit.DAY)
                Period(monday, monday.plus(6, DateTimeUnit.DAY))
            }
            RecurrenceType.MONTHLY -> {
                val firstDay = LocalDate(date.year, date.monthNumber, 1)
                val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                Period(firstDay, lastDay)
            }
        }
    }

    private fun previousPeriod(period: Period, type: RecurrenceType): Period {
        return when (type) {
            RecurrenceType.DAILY -> {
                val prev = period.startDate.minus(1, DateTimeUnit.DAY)
                Period(prev, prev)
            }
            RecurrenceType.WEEKLY -> {
                val prev = period.startDate.minus(7, DateTimeUnit.DAY)
                Period(prev, prev.plus(6, DateTimeUnit.DAY))
            }
            RecurrenceType.MONTHLY -> {
                val firstDay = period.startDate.minus(1, DateTimeUnit.MONTH)
                val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                Period(firstDay, lastDay)
            }
        }
    }

    private fun countCompletionsInPeriod(completedDates: Set<String>, period: Period): Int {
        return completedDates.count { dateStr ->
            val date = LocalDate.parse(dateStr)
            date >= period.startDate && date <= period.endDate
        }
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, today: LocalDate): Int {
        val completedDates = completions.map { it.completedDate }.toSet()
        var streak = 0
        var period = getPeriodContaining(today, habit.recurrenceType)

        // Search for the most recent completed period
        var searchLimit = 100
        var foundPeriod: Period? = null

        while (searchLimit-- > 0) {
            if (countCompletionsInPeriod(completedDates, period) >= habit.targetCount) {
                foundPeriod = period
                break
            }
            period = previousPeriod(period, habit.recurrenceType)
        }

        if (foundPeriod == null) return 0

        streak = 1
        var currentPeriod = previousPeriod(foundPeriod, habit.recurrenceType)

        // Continue walking backward through completed periods
        while (searchLimit-- > 0) {
            if (countCompletionsInPeriod(completedDates, currentPeriod) >= habit.targetCount) {
                streak++
                currentPeriod = previousPeriod(currentPeriod, habit.recurrenceType)
            } else {
                break
            }
        }

        return streak
    }

    private fun currentPeriodProgress(habit: Habit, completions: List<String>, today: LocalDate): Pair<Int, Int> {
        val period = getPeriodContaining(today, habit.recurrenceType)
        val count = countCompletionsInPeriod(completions.toSet(), period)
        return Pair(count, habit.targetCount)
    }

    private fun computeLast7Days(completions: List<String>, today: LocalDate): List<DayCompletion> {
        val completedDates = completions.toSet()
        return (6 downTo 0).map { daysAgo ->
            val date = today.minus(daysAgo, DateTimeUnit.DAY)
            DayCompletion(
                date = date.toString(),
                isCompleted = completedDates.contains(date.toString()),
                isScheduled = true
            )
        }
    }
}
```

---

## Task 3: Update Use Cases (`AddHabitUseCase`, `UpdateHabitUseCase`)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/AddHabitUseCase.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/UpdateHabitUseCase.kt`

### 3.1: Update `AddHabitUseCase`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/AddHabitUseCase.kt
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.datetime.Clock

class AddHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(
        name: String,
        recurrenceType: RecurrenceType,
        targetCount: Int,
        color: String
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val habit = Habit(
            id = randomUUID(),
            name = name.trim(),
            recurrenceType = recurrenceType,
            targetCount = targetCount,
            color = color,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        return repository.addHabit(habit)
    }

    private fun randomUUID(): String {
        return "h-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
    }
}
```

### 3.2: Update `UpdateHabitUseCase`

`UpdateHabitUseCase` uses `habit.toData()` and `repository.updateHabit()`, so it works automatically once the model and mapper are updated. No code changes needed — verify by reading the existing file and confirming it passes `habit` directly through.

---

## Task 4: Update `HabitViewModel` (Flat List State)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt
package com.programovil.aura.habit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.DeleteHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsWithStatusUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HabitListUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

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

class HabitViewModel(
    private val repository: HabitRepository,
    private val getHabitsWithStatusUseCase: GetHabitsWithStatusUseCase,
    private val addHabitUseCase: AddHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState: StateFlow<HabitListUiState> = _uiState

    init {
        loadHabits()
    }

    fun onEvent(event: HabitEvent) {
        when (event) {
            is HabitEvent.ToggleCompletion -> toggleCompletion(event.habitId, event.date)
            is HabitEvent.AddHabit -> addHabit(event.name, event.recurrenceType, event.targetCount, event.color)
            is HabitEvent.UpdateHabit -> updateHabit(event.habit)
            is HabitEvent.DeleteHabit -> deleteHabit(event.habitId)
        }
    }

    private fun loadHabits() {
        viewModelScope.launch {
            getHabitsWithStatusUseCase().collect { result ->
                result.onSuccess { habits ->
                    _uiState.value = HabitListUiState(
                        habits = habits,
                        isLoading = false
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load habits"
                    )
                }
            }
        }
    }

    private fun toggleCompletion(habitId: String, date: String) {
        viewModelScope.launch {
            toggleHabitCompletionUseCase(habitId, date)
                .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to update habit") }
        }
    }

    private fun addHabit(name: String, recurrenceType: RecurrenceType, targetCount: Int, color: String) {
        viewModelScope.launch {
            addHabitUseCase(name, recurrenceType, targetCount, color)
                .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to add habit") }
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
            deleteHabitUseCase(habitId)
                .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to delete habit") }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
```

---

## Task 5: Rewrite `HabitCard` (Replaces `HabitItem`)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitCard.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitCard.kt
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.shared.parseHexColor
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun HabitCard(
    habitWithStatus: HabitWithStatus,
    onToggle: (date: String) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStatus.habit
    val (completed, target) = habitWithStatus.currentPeriodProgress
    val streak = habitWithStatus.streak
    val last7Days = habitWithStatus.last7Days
    val habitColor = parseHexColor(habit.color)

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: color dot + name + progress + streak + today checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(habitColor)
                )

                Text(
                    text = habit.name,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Period progress (only for weekly/monthly)
                if (habit.recurrenceType != RecurrenceType.DAILY) {
                    Text(
                        text = "$completed/$target",
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary
                    )
                }

                // Streak
                if (streak > 0) {
                    Text(
                        text = streak.toString(),
                        style = AppTheme.typography.labelLarge,
                        color = AppTheme.colors.primary
                    )
                }

                // Today's checkbox
                val todayCompleted = last7Days.lastOrNull()?.isCompleted == true
                Checkbox(
                    checked = todayCompleted,
                    onCheckedChange = { onToggle(today.toString()) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = habitColor,
                        uncheckedColor = AppTheme.colors.textSecondary
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 7-day mini-grid
            SevenDayGrid(
                last7Days = last7Days,
                habitColor = habitColor,
                today = today,
                onToggle = onToggle
            )
        }
    }
}

@Composable
private fun SevenDayGrid(
    last7Days: List<DayCompletion>,
    habitColor: androidx.compose.ui.graphics.Color,
    today: kotlinx.datetime.LocalDate,
    onToggle: (date: String) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        last7Days.forEachIndexed { index, dayCompletion ->
            val isToday = dayCompletion.date == today.toString()
            DayCircle(
                dayCompletion = dayCompletion,
                dayLabel = dayLabels.getOrElse(index) { "" },
                habitColor = habitColor,
                isToday = isToday,
                onToggle = { onToggle(dayCompletion.date) }
            )
        }
    }
}

@Composable
private fun DayCircle(
    dayCompletion: DayCompletion,
    dayLabel: String,
    habitColor: androidx.compose.ui.graphics.Color,
    isToday: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (dayCompletion.isCompleted) habitColor
                    else AppTheme.colors.background
                )
                .then(
                    if (isToday) Modifier.border(2.dp, AppTheme.colors.textPrimary, CircleShape)
                    else Modifier.border(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.3f), CircleShape)
                )
                .clickable(onClick = onToggle)
        )
        Text(
            text = dayLabel,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary
        )
    }
}
```

---

## Task 6: Rewrite `HabitScreen` (Flat `LazyColumn`)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt
package com.programovil.aura.habit.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.presentation.composable.HabitCard
import com.programovil.aura.habit.presentation.composable.HabitDialog
import com.programovil.aura.habit.presentation.viewmodel.HabitEvent
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import kotlinx.datetime.*
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.habits_title
import aura_app.composeapp.generated.resources.add_habit
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
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

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
                    Column {
                        Text(stringResource(Res.string.habits_title))
                        Text(
                            text = today.toString(),
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        } else if (uiState.habits.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.habits, key = { it.habit.id }) { habitItem ->
                    HabitCard(
                        habitWithStatus = habitItem,
                        onToggle = { date ->
                            viewModel.onEvent(
                                HabitEvent.ToggleCompletion(habitItem.habit.id, date)
                            )
                        },
                        onLongClick = {
                            editingHabit = habitItem.habit
                            showDialog = true
                        }
                    )
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
```

---

## Task 7: Rewrite `HabitDialog` (Design System Fixes + New Recurrence)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitDialog.kt`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitDialog.kt
package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import aura_app.composeapp.generated.resources.delete_habit
import aura_app.composeapp.generated.resources.edit_habit
import aura_app.composeapp.generated.resources.habit_name
import aura_app.composeapp.generated.resources.monthly
import aura_app.composeapp.generated.resources.new_habit
import aura_app.composeapp.generated.resources.save
import aura_app.composeapp.generated.resources.weekly
import org.jetbrains.compose.resources.stringResource

private val colorPalette = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD"
)

@Composable
fun HabitDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (habit: Habit) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var recurrenceType by remember {
        mutableStateOf(habit?.recurrenceType ?: RecurrenceType.DAILY)
    }
    var targetCount by remember {
        mutableIntStateOf(habit?.targetCount ?: 1)
    }
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
                // Title with design system color fix
                Text(
                    text = stringResource(if (isEditMode) Res.string.edit_habit else Res.string.new_habit),
                    style = AppTheme.typography.headlineSmall,
                    color = AppTheme.colors.textPrimary  // FIXED: was missing
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

                // Recurrence selector: Daily | Weekly | Monthly
                RecurrenceSelector(
                    selected = recurrenceType,
                    onSelect = { recurrenceType = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target count stepper (only for weekly/monthly)
                if (recurrenceType != RecurrenceType.DAILY) {
                    TargetCountStepper(
                        count = targetCount,
                        max = if (recurrenceType == RecurrenceType.WEEKLY) 7 else 31,
                        onCountChange = { targetCount = it }
                    )
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
                        TextButton(onClick = {
                            onDelete()
                            onDismiss()
                        }) {
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
                                val habitToSave = Habit(
                                    id = habit?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    recurrenceType = recurrenceType,
                                    targetCount = if (recurrenceType == RecurrenceType.DAILY) 1 else targetCount,
                                    color = selectedColor,
                                    createdAt = habit?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(habitToSave)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurrenceSelector(
    selected: RecurrenceType,
    onSelect: (RecurrenceType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecurrenceType.entries.forEach { type ->
            val label = when (type) {
                RecurrenceType.DAILY -> stringResource(Res.string.daily)
                RecurrenceType.WEEKLY -> stringResource(Res.string.weekly)
                RecurrenceType.MONTHLY -> stringResource(Res.string.monthly)
            }
            val isSelected = type == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) AppTheme.colors.primary
                        else AppTheme.colors.surface.copy(alpha = 0.6f)
                    )
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
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

@Composable
private fun TargetCountStepper(
    count: Int,
    max: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = { if (count > 1) onCountChange(count - 1) },
            enabled = count > 1,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = if (count > 1) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = count.toString(),
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary
        )

        IconButton(
            onClick = { if (count < max) onCountChange(count + 1) },
            enabled = count < max,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = if (count < max) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = "× / ${if (max == 7) "week" else "month"}",
            style = AppTheme.typography.labelMedium,
            color = AppTheme.colors.textSecondary
        )
    }
}
```

---

## Task 8: Update DI Module

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt`

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
package com.programovil.aura.habit.di

import com.programovil.aura.habit.domain.repository.createHabitRepository
import com.programovil.aura.habit.domain.usecase.*
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val habitModule = module {
    single { createHabitRepository() }

    factoryOf(::GetHabitsWithStatusUseCase)
    factoryOf(::AddHabitUseCase)
    factoryOf(::UpdateHabitUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::DeleteHabitUseCase)

    factoryOf(::HabitViewModel)
}
```

---

## Task 9: Update String Resources

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

Add new strings and remove unused ones. Remove `repeat_on`, `day_mon` through `day_sun`, `daily_badge`. Add `recurrence_monthly`.

```xml
<!-- In strings.xml, add under Habit Screen section: -->
<string name="recurrence_monthly">Monthly</string>
```

Remove: `repeat_on`, `day_mon`, `day_tue`, `day_wed`, `day_thu`, `day_fri`, `day_sat`, `day_sun`, `daily_badge`.

---

## Task 10: Update Tests

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/model/RecurrenceTypeTest.kt`
- Delete: `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/domain/model/DaySectionTest.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModelTest.kt`

### 10.1: Update `RecurrenceTypeTest`

Add `MONTHLY` to the test.

### 10.2: Delete `DaySectionTest.kt`

Delete the file entirely.

### 10.3: Update `HabitViewModelTest`

Rewrite for flat list state. Update assertions to check `uiState.habits` (flat list) instead of `uiState.todayHabits`, `uiState.tomorrowHabits`, etc.

---

## Execution Order

1. **Task 1** (Domain models — foundation for everything)
2. **Task 2** (New use case — needs domain models)
3. **Task 3** (Use case updates — needs domain models)
4. **Task 4** (ViewModel — needs Task 2)
5. **Task 5** (HabitCard UI — needs Task 4 for state shape)
6. **Task 6** (HabitScreen — needs Task 5)
7. **Task 7** (HabitDialog — mostly standalone, needs Task 1 model)
8. **Task 8** (DI — needs Tasks 2, 3, 4)
9. **Task 9** (Strings — needs Tasks 5, 6, 7)
10. **Task 10** (Tests — update for new model)

**Parallelism:** Task 7 (Dialog) can be done in parallel with Tasks 4-6 since it doesn't depend on UI state changes. Tasks 9 (Strings) and 10 (Tests) can be started after Task 1 is complete.
