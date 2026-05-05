# Habit Room-to-Firestore Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Room KMP from the habit feature and replace it with a fully online Firestore implementation matching the existing todo feature pattern.

**Architecture:** Expect/actual repository factory pattern with Android Firestore actuals and iOS no-op stubs. Domain use cases consume `Flow<Result<...>>` streams and unwrap them. Presentation layer handles `Result` in the ViewModel.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Firebase Firestore, Koin DI

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `composeApp/build.gradle.kts` | Modify | Remove Room plugin, runtime, KSP processors, `room { }` block |
| `gradle/libs.versions.toml` | Modify | Remove Room version, libraries, and plugin entries |
| `habit/data/local/*` | Delete | All Room entities, DAOs, database, platform builders |
| `habit/data/mapper/HabitMapper.kt` | Delete | Room entity-to-domain mapper |
| `habit/data/repository/HabitRepositoryImpl.kt` (commonMain) | Delete | Room-based repository implementation |
| `habit/domain/repository/HabitRepository.kt` | Modify | Add `expect` factory, wrap returns in `Result`, remove `cleanupOldCompletions` |
| `habit/data/mapper/HabitData.kt` | Create | Firestore DTOs (`HabitData`, `HabitCompletionData`) and `toDomain()`/`toData()` mappers |
| `habit/data/repository/HabitRepositoryImpl.kt` (androidMain) | Create | Firestore `actual` implementation |
| `habit/data/repository/HabitRepositoryImpl.kt` (iosMain) | Create | iOS no-op stub `actual` implementation |
| `habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt` | Modify | Handle `Result` wrappers from both combined flows |
| `habit/domain/usecase/GetHabitHistoryUseCase.kt` | Modify | Return `Flow<Result<List<HabitCompletion>>>` |
| `habit/di/HabitModule.kt` | Modify | Replace DB singletons with `createHabitRepository()` factory |
| `habit/presentation/viewmodel/HabitViewModel.kt` | Modify | Remove `cleanupOldCompletions` call, handle `Result` in `loadHabits()` |

---

## Task 1: Remove Room from Build System

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1.1: Remove Room plugin from `composeApp/build.gradle.kts`**

Remove this line from the `plugins` block:
```kotlin
    alias(libs.plugins.room)
```

- [ ] **Step 1.2: Remove Room runtime dependency**

Remove these lines from `commonMain.dependencies`:
```kotlin
            // Room KMP core runtime
            implementation(libs.room.runtime)
            // REMOVED: implementation(libs.room.ktx)

            // Required for iOS SQLite execution
            implementation(libs.androidx.sqlite.bundled)
```

- [ ] **Step 1.3: Remove Room KSP processors and DSL block**

Remove these lines from the bottom of the file:
```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}
```

Replace with:
```kotlin
dependencies {
    debugImplementation(libs.compose.uiTooling)
}
```

- [ ] **Step 1.4: Remove Room entries from `gradle/libs.versions.toml`**

Remove from `[versions]`:
```toml
room = "2.8.4"
```

Remove from `[libraries]`:
```toml
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
```

Remove from `[plugins]`:
```toml
room = { id = "androidx.room", version.ref = "room" }
```

- [ ] **Step 1.5: Commit**

```bash
git add composeApp/build.gradle.kts gradle/libs.versions.toml
git commit -m "build: remove Room dependencies and KSP processors"
```

---

## Task 2: Delete All Room Files

**Files:**
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitEntity.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitCompletionEntity.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitDao.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitCompletionDao.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitMapper.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`
- Delete: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.android.kt`
- Delete: `composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.ios.kt`

- [ ] **Step 2.1: Delete the files**

```bash
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitEntity.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitCompletionEntity.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitDao.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitCompletionDao.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitMapper.kt
rm composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
rm composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.android.kt
rm composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.ios.kt
```

- [ ] **Step 2.2: Remove empty directories**

```bash
rmdir composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity 2>/dev/null || true
rmdir composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local 2>/dev/null || true
rmdir composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local 2>/dev/null || true
rmdir composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/local 2>/dev/null || true
```

- [ ] **Step 2.3: Commit**

```bash
git add -A
git commit -m "refactor(habit): delete all Room entities, DAOs, database, and mappers"
```

---

## Task 3: Update HabitRepository Interface

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt`

- [ ] **Step 3.1: Rewrite the interface with Result wrappers and expect factory**

Replace the entire file with:

```kotlin
package com.programovil.aura.habit.domain.repository

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun getHabits(): Flow<Result<List<Habit>>>
    fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>>
    fun getAllCompletions(): Flow<Result<List<HabitCompletion>>>
    suspend fun addHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun toggleCompletion(habitId: String, date: String): Result<Unit>
}

expect fun createHabitRepository(): HabitRepository
```

- [ ] **Step 3.2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt
git commit -m "refactor(habit): add expect factory and Result wrappers to HabitRepository"
```

---

## Task 4: Create HabitData DTOs

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitData.kt`

- [ ] **Step 4.1: Write the DTOs and mappers**

```kotlin
package com.programovil.aura.habit.data.mapper

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType

data class HabitData(
    val id: String,
    val name: String,
    val recurrenceType: String,
    val daysOfWeek: List<Int> = emptyList(),
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
    daysOfWeek = daysOfWeek,
    color = color,
    createdAt = createdAt ?: 0L
)

fun Habit.toData(): HabitData = HabitData(
    id = id,
    name = name,
    recurrenceType = recurrenceType.name,
    daysOfWeek = daysOfWeek,
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

- [ ] **Step 4.2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitData.kt
git commit -m "feat(habit): add Firestore DTOs and domain mappers"
```

---

## Task 5: Update Use Cases for Result Wrappers

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitHistoryUseCase.kt`

- [ ] **Step 5.1: Update `GetHabitsGroupedByDayUseCase`**

Replace the entire file:

```kotlin
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.*

class GetHabitsGroupedByDayUseCase(private val repository: HabitRepository) {

    operator fun invoke(): Flow<Result<Map<DaySection, List<HabitWithStatus>>>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        return combine(
            repository.getHabits(),
            repository.getAllCompletions()
        ) { habitsResult, completionsResult ->
            habitsResult.fold(
                onSuccess = { habits ->
                    completionsResult.fold(
                        onSuccess = { completions ->
                            Result.success(buildMap {
                                put(DaySection.TODAY, groupHabitsForDate(habits, completions, today))
                                put(DaySection.TOMORROW, groupHabitsForDate(habits, completions, today.plus(1, DateTimeUnit.DAY)))
                                put(DaySection.THIS_WEEK, buildThisWeekHabits(habits, completions, today))
                            })
                        },
                        onFailure = { Result.failure(it) }
                    )
                },
                onFailure = { Result.failure(it) }
            )
        }
    }

    private fun groupHabitsForDate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        date: LocalDate
    ): List<HabitWithStatus> {
        val dayOfWeek = date.dayOfWeek.isoDayNumber // 1=Monday, 7=Sunday
        val dateStr = date.toString()

        return habits
            .filter { it.isScheduledFor(dayOfWeek) }
            .map { habit ->
                val isDone = completions.any { it.habitId == habit.id && it.completedDate == dateStr }
                val streak = calculateStreak(habit, completions, date)
                HabitWithStatus(
                    habit = habit,
                    isDone = isDone,
                    isMissed = !isDone && date < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    streak = streak,
                    targetDate = dateStr
                )
            }
    }

    private fun buildThisWeekHabits(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate
    ): List<HabitWithStatus> {
        val result = mutableListOf<HabitWithStatus>()
        // Show next 7 days starting from day after tomorrow
        for (i in 2..7) {
            val date = today.plus(i, DateTimeUnit.DAY)
            result.addAll(groupHabitsForDate(habits, completions, date))
        }
        return result
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, fromDate: LocalDate): Int {
        var streak = 0
        var currentDate = fromDate
        val completedDates = completions.filter { it.habitId == habit.id }.map { it.completedDate }.toSet()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Look back up to 90 days
        repeat(90) {
            if (habit.isScheduledFor(currentDate.dayOfWeek.isoDayNumber)) {
                val dateStr = currentDate.toString()
                if (completedDates.contains(dateStr)) {
                    streak++
                } else if (currentDate <= today) {
                    return streak // streak broken
                }
            }
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun Habit.isScheduledFor(dayOfWeek: Int): Boolean {
        return recurrenceType == RecurrenceType.DAILY || daysOfWeek.contains(dayOfWeek)
    }
}
```

- [ ] **Step 5.2: Update `GetHabitHistoryUseCase`**

Replace the entire file:

```kotlin
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetHabitHistoryUseCase(private val repository: HabitRepository) {
    operator fun invoke(habitId: String): Flow<Result<List<HabitCompletion>>> {
        return repository.getCompletionsForHabit(habitId)
    }
}
```

- [ ] **Step 5.3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitHistoryUseCase.kt
git commit -m "refactor(habit): update use cases to handle Result-wrapped Firestore flows"
```

---

## Task 6: Create Android Firestore Repository

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`

- [ ] **Step 6.1: Write the Android actual implementation**

```kotlin
package com.programovil.aura.habit.domain.repository

import com.programovil.aura.habit.data.mapper.HabitCompletionData
import com.programovil.aura.habit.data.mapper.HabitData
import com.programovil.aura.habit.data.mapper.toData
import com.programovil.aura.habit.data.mapper.toDomain
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.shared.FirebaseConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual fun createHabitRepository(): HabitRepository = HabitRepositoryImpl()

private class HabitRepositoryImpl : HabitRepository {

    private val userId: String
        get() = FirebaseConfig.auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

    private fun userHabitsCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("habits")

    private fun userCompletionsCollection() = FirebaseConfig.firestore
        .collection("users").document(userId).collection("completions")

    override fun getHabits(): Flow<Result<List<Habit>>> = callbackFlow {
        val listener = userHabitsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val habits = snapshot?.documents?.mapNotNull { doc ->
                    val daysOfWeek = (doc.get("daysOfWeek") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
                    val habitData = HabitData(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        recurrenceType = doc.getString("recurrenceType") ?: "DAILY",
                        daysOfWeek = daysOfWeek,
                        color = doc.getString("color") ?: "",
                        createdAt = doc.getLong("createdAt")
                    )
                    habitData.toDomain()
                } ?: emptyList()
                trySend(Result.success(habits))
            }
        awaitClose { listener.remove() }
    }

    override fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>> = callbackFlow {
        val listener = userCompletionsCollection()
            .whereEqualTo("habitId", habitId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val completions = snapshot?.documents?.mapNotNull { doc ->
                    val completionData = HabitCompletionData(
                        id = doc.id,
                        habitId = doc.getString("habitId") ?: "",
                        completedDate = doc.getString("completedDate") ?: "",
                        completedAt = doc.getLong("completedAt")
                    )
                    completionData.toDomain()
                } ?: emptyList()
                trySend(Result.success(completions))
            }
        awaitClose { listener.remove() }
    }

    override fun getAllCompletions(): Flow<Result<List<HabitCompletion>>> = callbackFlow {
        val listener = userCompletionsCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val completions = snapshot?.documents?.mapNotNull { doc ->
                    val completionData = HabitCompletionData(
                        id = doc.id,
                        habitId = doc.getString("habitId") ?: "",
                        completedDate = doc.getString("completedDate") ?: "",
                        completedAt = doc.getLong("completedAt")
                    )
                    completionData.toDomain()
                } ?: emptyList()
                trySend(Result.success(completions))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addHabit(habit: Habit): Result<Unit> = runCatching {
        val data = mapOf(
            "name" to habit.name,
            "recurrenceType" to habit.recurrenceType.name,
            "daysOfWeek" to habit.daysOfWeek,
            "color" to habit.color,
            "createdAt" to habit.createdAt
        )
        userHabitsCollection().document(habit.id).set(data).await()
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        userHabitsCollection().document(habitId).delete().await()
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        val query = userCompletionsCollection()
            .whereEqualTo("habitId", habitId)
            .whereEqualTo("completedDate", date)
            .get()
            .await()

        if (!query.isEmpty) {
            query.documents.first().reference.delete().await()
        } else {
            val data = mapOf(
                "habitId" to habitId,
                "completedDate" to date,
                "completedAt" to System.currentTimeMillis()
            )
            userCompletionsCollection().add(data).await()
        }
    }
}
```

- [ ] **Step 6.2: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
git commit -m "feat(habit): implement Android Firestore repository actual"
```

---

## Task 7: Create iOS Stub Repository

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`

- [ ] **Step 7.1: Write the iOS stub**

```kotlin
package com.programovil.aura.habit.domain.repository

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createHabitRepository(): HabitRepository = IosHabitRepositoryImpl()

private class IosHabitRepositoryImpl : HabitRepository {
    override fun getHabits(): Flow<Result<List<Habit>>> = flowOf(Result.success(emptyList()))
    override fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>> = flowOf(Result.success(emptyList()))
    override fun getAllCompletions(): Flow<Result<List<HabitCompletion>>> = flowOf(Result.success(emptyList()))
    override suspend fun addHabit(habit: Habit): Result<Unit> = Result.success(Unit)
    override suspend fun deleteHabit(habitId: String): Result<Unit> = Result.success(Unit)
    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = Result.success(Unit)
}
```

- [ ] **Step 7.2: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
git commit -m "feat(habit): add iOS no-op stub repository actual"
```

---

## Task 8: Update DI Module

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt`

- [ ] **Step 8.1: Replace Room DI with factory pattern**

Replace the entire file:

```kotlin
package com.programovil.aura.habit.di

import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.repository.createHabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val habitModule = module {
    // Data layer
    single { createHabitRepository() }

    // Domain layer - use cases
    factoryOf(::AddHabitUseCase)
    factoryOf(::GetHabitsGroupedByDayUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::GetHabitHistoryUseCase)

    // Presentation layer
    viewModelOf(::HabitViewModel)
}
```

- [ ] **Step 8.2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
git commit -m "refactor(habit): replace Room DB DI with createHabitRepository factory"
```

---

## Task 9: Update ViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt`

- [ ] **Step 9.1: Remove cleanup and handle Result**

Replace the entire file:

```kotlin
package com.programovil.aura.habit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HabitListUiState(
    val todayHabits: List<HabitWithStatus> = emptyList(),
    val tomorrowHabits: List<HabitWithStatus> = emptyList(),
    val thisWeekHabits: List<HabitWithStatus> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class HabitEvent {
    data class ToggleCompletion(val habitId: String, val date: String) : HabitEvent()
    data class AddHabit(
        val name: String,
        val recurrenceType: RecurrenceType,
        val daysOfWeek: List<Int>,
        val color: String
    ) : HabitEvent()
}

class HabitViewModel(
    private val repository: HabitRepository,
    private val getHabitsGroupedByDayUseCase: GetHabitsGroupedByDayUseCase,
    private val addHabitUseCase: AddHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState: StateFlow<HabitListUiState> = _uiState

    init {
        loadHabits()
    }

    fun onEvent(event: HabitEvent) {
        when (event) {
            is HabitEvent.ToggleCompletion -> toggleCompletion(event.habitId, event.date)
            is HabitEvent.AddHabit -> addHabit(event.name, event.recurrenceType, event.daysOfWeek, event.color)
        }
    }

    private fun loadHabits() {
        viewModelScope.launch {
            getHabitsGroupedByDayUseCase().collect { result ->
                result.onSuccess { grouped ->
                    _uiState.value = HabitListUiState(
                        todayHabits = grouped[DaySection.TODAY] ?: emptyList(),
                        tomorrowHabits = grouped[DaySection.TOMORROW] ?: emptyList(),
                        thisWeekHabits = grouped[DaySection.THIS_WEEK] ?: emptyList(),
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

    private fun addHabit(name: String, recurrenceType: RecurrenceType, daysOfWeek: List<Int>, color: String) {
        viewModelScope.launch {
            addHabitUseCase(name, recurrenceType, daysOfWeek, color)
                .onFailure { _uiState.value = _uiState.value.copy(error = "Failed to add habit") }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getTodayDate(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
}
```

- [ ] **Step 9.2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt
git commit -m "refactor(habit): remove cleanup, handle Result in ViewModel"
```

---

## Task 10: Build Verification

**Files:**
- None (verification only)

- [ ] **Step 10.1: Verify Android compilation**

Run:
```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

Expected: Build succeeds with no Room-related errors.

- [ ] **Step 10.2: Verify commonMain compilation**

Run:
```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

Expected: Build succeeds.

- [ ] **Step 10.3: Check for broken imports**

Run:
```bash
./gradlew :composeApp:build
```

If the build fails, inspect the error message. Likely causes:
- Missing import in `HabitModule.kt` (should import `createHabitRepository`)
- Missing import in `HabitRepositoryImpl.kt` Android actual (should import `FirebaseConfig`)
- Type mismatch in use cases (should be `Result`-wrapped flows)

- [ ] **Step 10.4: Final commit**

```bash
git add -A
git commit -m "refactor(habit): complete Room-to-Firestore migration"
```

---

## Self-Review Checklist

### Spec Coverage

| Spec Requirement | Implementing Task |
|------------------|-------------------|
| Remove Room from build | Task 1 |
| Delete all Room files | Task 2 |
| Add `expect fun createHabitRepository()` | Task 3 |
| Wrap repository returns in `Result` | Task 3 |
| Remove `cleanupOldCompletions` | Task 3, Task 9 |
| Create Firestore DTOs | Task 4 |
| Android Firestore actual | Task 6 |
| iOS no-op stub | Task 7 |
| Update use cases for `Result` | Task 5 |
| Update DI module | Task 8 |
| Update ViewModel | Task 9 |
| Firestore schema (habits + completions) | Task 6 |
| Build verification | Task 10 |

### Placeholder Scan

- [x] No "TBD", "TODO", or "implement later"
- [x] No vague "add error handling" without code
- [x] No "similar to Task N" shortcuts
- [x] Every step has exact file paths
- [x] Every code-changing step has a complete code block

### Type Consistency

- [x] `HabitRepository.getHabits()` returns `Flow<Result<List<Habit>>>` everywhere
- [x] `HabitRepository.getAllCompletions()` returns `Flow<Result<List<HabitCompletion>>>` everywhere
- [x] `GetHabitsGroupedByDayUseCase.invoke()` returns `Flow<Result<Map<DaySection, List<HabitWithStatus>>>>` everywhere
- [x] `GetHabitHistoryUseCase.invoke()` returns `Flow<Result<List<HabitCompletion>>>` everywhere
- [x] `createHabitRepository()` returns `HabitRepository` in commonMain expect and both actuals
- [x] `HabitModule` uses `single { createHabitRepository() }` (no `.bind<>()` needed since factory returns interface)

### Known Gaps

- No unit tests exist for this layer in the project. Verification is manual/build-based only.
- iOS linker flag `-lsqlite3` remains; it is harmless but was originally added for Room. If iOS build fails, remove it from `composeApp/build.gradle.kts` in the `iosTarget.binaries.framework` block.
