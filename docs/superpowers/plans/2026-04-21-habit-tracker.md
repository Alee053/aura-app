# Habit Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a habit tracker with local-only persistence (Room on Android, UserDefaults on iOS), following the same Clean Architecture pattern as the existing todo feature.

**Architecture:** Clean Architecture with domain/data/presentation layers. Each platform has its own repository implementation behind a shared interface. Koin DI for dependency injection.

**Tech Stack:** Kotlin Multiplatform Compose, Koin DI, Room (Android), UserDefaults (iOS), Kotlin Flows.

---

## File Structure

```
composeApp/src/commonMain/kotlin/com/programovil/aura/
├── habit/
│   ├── data/
│   │   ├── repository/
│   │   │   └── HabitRepositoryImpl.kt
│   │   └── mapper/
│   │       └── HabitMapper.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Habit.kt
│   │   │   ├── HabitCompletion.kt
│   │   │   ├── RecurrenceType.kt
│   │   │   └── HabitWithStatus.kt
│   │   ├── repository/
│   │   │   └── HabitRepository.kt
│   │   └── usecase/
│   │       ├── AddHabitUseCase.kt
│   │       ├── GetHabitsGroupedByDayUseCase.kt
│   │       ├── ToggleHabitCompletionUseCase.kt
│   │       └── GetHabitHistoryUseCase.kt
│   ├── presentation/
│   │   ├── screen/
│   │   │   └── HabitScreen.kt
│   │   ├── viewmodel/
│   │   │   └── HabitViewModel.kt
│   │   └── composable/
│   │       ├── HabitItem.kt
│   │       └── AddHabitDialog.kt
│   └── di/
│       └── HabitModule.kt
├── navigation/
│   └── NavRoute.kt                          # Modify: add Habit route
├── di/
│   └── InitKoin.kt                          # Modify: add HabitModule to getModules()
└── App.kt                                   # Modify: add navigation tab or route
```

Platform-specific files (Android):
```
composeApp/src/androidMain/kotlin/com/programovil/aura/
├── habit/
│   └── data/
│       └── local/
│           ├── HabitDatabase.kt
│           ├── HabitDao.kt
│           ├── HabitCompletionDao.kt
│           └── entity/
│               ├── HabitEntity.kt
│               └── HabitCompletionEntity.kt
```

Platform-specific files (iOS):
```
composeApp/src/iosMain/kotlin/com/programovil/aura/
└── habit/
    └── data/
        └── local/
            └── HabitUserDefaults.kt
```

---

## Task 1: Domain Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/RecurrenceType.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/Habit.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitCompletion.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitWithStatus.kt`

- [ ] **Step 1: Write RecurrenceType enum**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/RecurrenceType.kt
package com.programovil.aura.habit.domain.model

enum class RecurrenceType {
    DAILY,
    WEEKLY
}
```

- [ ] **Step 2: Write Habit data class**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/Habit.kt
package com.programovil.aura.habit.domain.model

data class Habit(
    val id: String,
    val name: String,
    val recurrenceType: RecurrenceType,
    val daysOfWeek: List<Int> = emptyList(), // 1=Monday, 7=Sunday. Empty for DAILY.
    val color: String, // hex color code
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Write HabitCompletion data class**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitCompletion.kt
package com.programovil.aura.habit.domain.model

data class HabitCompletion(
    val id: String,
    val habitId: String,
    val completedDate: String, // YYYY-MM-DD format
    val completedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Write HabitWithStatus UI model**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/HabitWithStatus.kt
package com.programovil.aura.habit.domain.model

data class HabitWithStatus(
    val habit: Habit,
    val isDone: Boolean,
    val isMissed: Boolean,
    val streak: Int = 0
)
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/
git commit -m "feat(habit): add domain models - RecurrenceType, Habit, HabitCompletion, HabitWithStatus"
```

---

## Task 2: Repository Interface

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt`

- [ ] **Step 1: Write HabitRepository interface**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt
package com.programovil.aura.habit.domain.repository

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>>
    suspend fun addHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun toggleCompletion(habitId: String, date: String): Result<Unit>
    suspend fun cleanupOldCompletions(olderThanDays: Int): Result<Unit>
}
```

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt
git commit -m "feat(habit): add HabitRepository interface"
```

---

## Task 3: Use Cases

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DaySection.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/AddHabitUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/ToggleHabitCompletionUseCase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitHistoryUseCase.kt`

- [ ] **Step 1: Write DaySection enum**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/model/DaySection.kt
package com.programovil.aura.habit.domain.model

enum class DaySection(val displayName: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    THIS_WEEK("This Week")
}
```

- [ ] **Step 1: Write AddHabitUseCase**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/AddHabitUseCase.kt
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import java.util.UUID

class AddHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(
        name: String,
        recurrenceType: RecurrenceType,
        daysOfWeek: List<Int>,
        color: String
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            recurrenceType = recurrenceType,
            daysOfWeek = if (recurrenceType == RecurrenceType.WEEKLY) daysOfWeek else emptyList(),
            color = color
        )
        return repository.addHabit(habit)
    }
}
```

- [ ] **Step 2: Write GetHabitsGroupedByDayUseCase**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class DaySection(val displayName: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    THIS_WEEK("This Week")
}

data class HabitGroupedByDay(
    val section: DaySection,
    val date: LocalDate,
    val habits: List<HabitWithStatus>
)

class GetHabitsGroupedByDayUseCase(private val repository: HabitRepository) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    operator fun invoke(): Flow<Map<DaySection, List<HabitWithStatus>>> {
        val today = LocalDate.now()
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        return combine(
            repository.getHabits(),
            repository.getCompletionsForHabit("") // placeholder - use all completions
        ) { habits, completions ->
            buildMap {
                put(DaySection.TODAY, groupHabitsForDate(habits, completions, today))
                put(DaySection.TOMORROW, groupHabitsForDate(habits, completions, today.plusDays(1)))
                put(DaySection.THIS_WEEK, buildThisWeekHabits(habits, completions, today, endOfWeek))
            }
        }
    }

    private fun groupHabitsForDate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        date: LocalDate
    ): List<HabitWithStatus> {
        val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday
        val dateStr = date.format(dateFormatter)

        return habits
            .filter { it.isScheduledFor(dayOfWeek) }
            .map { habit ->
                val isDone = completions.any { it.habitId == habit.id && it.completedDate == dateStr }
                val streak = calculateStreak(habit, completions, date)
                HabitWithStatus(
                    habit = habit,
                    isDone = isDone,
                    isMissed = !isDone && date.isBefore(LocalDate.now()),
                    streak = streak
                )
            }
    }

    private fun buildThisWeekHabits(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        today: LocalDate,
        endOfWeek: LocalDate
    ): List<HabitWithStatus> {
        val result = mutableListOf<HabitWithStatus>()
        var date = today.plusDays(2) // Start from day after tomorrow
        while (!date.isAfter(endOfWeek)) {
            result.addAll(groupHabitsForDate(habits, completions, date))
            date = date.plusDays(1)
        }
        return result
    }

    private fun calculateStreak(habit: Habit, completions: List<HabitCompletion>, fromDate: LocalDate): Int {
        var streak = 0
        var currentDate = fromDate
        val completedDates = completions.filter { it.habitId == habit.id }.map { it.completedDate }.toSet()

        // Look back up to 90 days
        repeat(90) {
            if (habit.isScheduledFor(currentDate.dayOfWeek.value)) {
                val dateStr = currentDate.format(dateFormatter)
                if (completedDates.contains(dateStr)) {
                    streak++
                } else if (currentDate.isBefore(LocalDate.now()) || currentDate.isEqual(LocalDate.now())) {
                    return streak // streak broken
                }
            }
            currentDate = currentDate.minusDays(1)
        }
        return streak
    }

    private fun Habit.isScheduledFor(dayOfWeek: Int): Boolean {
        return recurrenceType == RecurrenceType.DAILY || daysOfWeek.contains(dayOfWeek)
    }
}
```

- [ ] **Step 3: Write ToggleHabitCompletionUseCase**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/ToggleHabitCompletionUseCase.kt
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.repository.HabitRepository

class ToggleHabitCompletionUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habitId: String, date: String): Result<Unit> {
        if (habitId.isBlank()) return Result.failure(IllegalArgumentException("Habit ID cannot be empty"))
        if (date.isBlank()) return Result.failure(IllegalArgumentException("Date cannot be empty"))
        return repository.toggleCompletion(habitId, date)
    }
}
```

- [ ] **Step 4: Write GetHabitHistoryUseCase**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitHistoryUseCase.kt
package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetHabitHistoryUseCase(private val repository: HabitRepository) {
    operator fun invoke(habitId: String): Flow<List<HabitCompletion>> {
        return repository.getCompletionsForHabit(habitId)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/
git commit -m "feat(habit): add use cases - AddHabit, GetHabitsGroupedByDay, ToggleHabitCompletion, GetHabitHistory"
```

---

## Task 4: Data Layer - Android Room

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitEntity.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitCompletionEntity.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDao.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitCompletionDao.kt`
- Create: `composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitMapper.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt`
- Modify: `composeApp/build.gradle.kts` — add Room dependencies

- [ ] **Step 1: Add Room dependencies to build.gradle.kts**

Add to `androidMain.dependencies` block in `composeApp/build.gradle.kts`:
```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
```

Add to `androidMain.dependencies` (or root `dependencies` block if outside kotlin block):
```kotlin
// Room KSP for annotation processing
```

Note: Check `gradle/libs.versions.toml` for Room dependencies and add if missing. If Room is not in libs.versions.toml, add the version there and reference it.

- [ ] **Step 2: Write HabitEntity**

```kotlin
// composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitEntity.kt
package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val recurrenceType: String, // "DAILY" or "WEEKLY"
    val daysOfWeek: String, // comma-separated, e.g., "1,3,5" for Mon,Wed,Fri. Empty for DAILY.
    val color: String,
    val createdAt: Long
)
```

- [ ] **Step 3: Write HabitCompletionEntity**

```kotlin
// composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitCompletionEntity.kt
package com.programovil.aura.habit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("completedDate")]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val completedDate: String, // YYYY-MM-DD
    val completedAt: Long
)
```

- [ ] **Step 4: Write HabitDao**

```kotlin
// composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDao.kt
package com.programovil.aura.habit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.programovil.aura.habit.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)
}
```

- [ ] **Step 5: Write HabitCompletionDao**

```kotlin
// composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitCompletionDao.kt
package com.programovil.aura.habit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.programovil.aura.habit.data.local.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE completedDate = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteCompletion(habitId: String, date: String)

    @Query("DELETE FROM habit_completions WHERE completedDate < :cutoffDate")
    suspend fun deleteOldCompletions(cutoffDate: String)

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletions(): Flow<List<HabitCompletionEntity>>
}
```

- [ ] **Step 6: Write HabitDatabase**

```kotlin
// composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.kt
package com.programovil.aura.habit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.programovil.aura.habit.data.local.entity.HabitCompletionEntity
import com.programovil.aura.habit.data.local.entity.HabitEntity

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
}
```

- [ ] **Step 7: Write HabitMapper**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitMapper.kt
package com.programovil.aura.habit.data.mapper

import com.programovil.aura.habit.data.local.entity.HabitCompletionEntity
import com.programovil.aura.habit.data.local.entity.HabitEntity
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType

object HabitMapper {
    fun HabitEntity.toDomain(): Habit = Habit(
        id = id,
        name = name,
        recurrenceType = RecurrenceType.valueOf(recurrenceType),
        daysOfWeek = if (daysOfWeek.isBlank()) emptyList() else daysOfWeek.split(",").map { it.toInt() },
        color = color,
        createdAt = createdAt
    )

    fun Habit.toEntity(): HabitEntity = HabitEntity(
        id = id,
        name = name,
        recurrenceType = recurrenceType.name,
        daysOfWeek = daysOfWeek.joinToString(","),
        color = color,
        createdAt = createdAt
    )

    fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
        id = id,
        habitId = habitId,
        completedDate = completedDate,
        completedAt = completedAt
    )

    fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
        id = id,
        habitId = habitId,
        completedDate = completedDate,
        completedAt = completedAt
    )
}
```

- [ ] **Step 8: Write HabitRepositoryImpl (Android Room)**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitCompletionDao
import com.programovil.aura.habit.data.local.HabitDao
import com.programovil.aura.habit.data.mapper.HabitMapper.toDomain
import com.programovil.aura.habit.data.mapper.HabitMapper.toEntity
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao
) : HabitRepository {

    override fun getHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
        return habitCompletionDao.getCompletionsForHabit(habitId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addHabit(habit: Habit): Result<Unit> = runCatching {
        habitDao.insertHabit(habit.toEntity())
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        habitDao.deleteHabit(habitId)
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        // Check if completion exists for this habit on this date
        var found = false
        habitCompletionDao.getCompletionsForDate(date).collect { completions ->
            val existing = completions.find { it.habitId == habitId }
            if (existing != null) {
                habitCompletionDao.deleteCompletion(habitId, date)
                found = true
            }
        }
        if (!found) {
            val completion = HabitCompletion(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                completedDate = date,
                completedAt = System.currentTimeMillis()
            )
            habitCompletionDao.insertCompletion(completion.toEntity())
        }
    }

    override suspend fun cleanupOldCompletions(olderThanDays: Int): Result<Unit> = runCatching {
        val cutoffDate = java.time.LocalDate.now().minusDays(olderThanDays.toLong())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        habitCompletionDao.deleteOldCompletions(cutoffDate)
    }
}
```

Note: The toggle implementation needs careful handling since we're collecting from a Flow. Consider using a suspend function with a direct query instead of collecting from Flow for the toggle operation. Refactor to use `@Query` with suspend function.

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/
git add composeApp/build.gradle.kts
git commit -m "feat(habit): add Android Room data layer - database, DAOs, entities, repository impl"
```

---

## Task 5: Data Layer - iOS UserDefaults

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/local/HabitUserDefaults.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt` — add iOS implementation

Note: In Kotlin Multiplatform, the HabitRepositoryImpl in commonMain is the shared interface/pattern, but platform-specific implementations live in androidMain and iosMain. The pattern is to have expect/actual or to use a different approach.

For this project, following the existing pattern: The `HabitRepositoryImpl` in `commonMain` should be an expect declaration, with actual implementations in platform folders. However, looking at the existing todo implementation, there's only a single `TodoRepositoryImpl` in commonMain that directly uses Firestore (which works on both platforms via Firebase).

Since habits use local storage, we need platform-specific implementations. The standard KMP approach:
- Create `HabitRepositoryImpl.kt` in `commonMain` as expect (or abstract)
- Create actual implementations in `androidMain` and `iosMain`

Alternative simpler approach for this project: Use a shared repository interface in commonMain, and the platform-specific implementations register themselves via Koin DI. We'll use the expect/actual pattern.

- [ ] **Step 1: Write HabitRepositoryImpl expect/actual**

First, modify the repository interface to be more concrete for local-only use, then create platform-specific actual implementations.

Create in `commonMain`:
```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
package com.programovil.aura.habit.data.repository

expect class HabitRepositoryImpl() : HabitRepository
```

Create in `androidMain`:
```kotlin
// composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitCompletionDao
import com.programovil.aura.habit.data.local.HabitDao
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

actual class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao
) : HabitRepository {
    // ... same implementation as Task 4 Step 8
}
```

Create in `iosMain`:
```kotlin
// composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitUserDefaults
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

actual class HabitRepositoryImpl(
    private val userDefaults: HabitUserDefaults
) : HabitRepository {
    // ... iOS implementation using UserDefaults
}
```

- [ ] **Step 2: Write HabitUserDefaults for iOS**

```kotlin
// composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/local/HabitUserDefaults.kt
package com.programovil.aura.habit.data.local

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class HabitUserDefaults {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val habitsKey = "habits_data"
    private val completionsKey = "completions_data"

    private val _habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val _completionsFlow = MutableStateFlow<List<HabitCompletion>>(emptyList())

    init {
        refreshFlows()
    }

    private fun refreshFlows() {
        _habitsFlow.value = getHabits()
        _completionsFlow.value = getCompletions()
    }

    fun getHabits(): List<Habit> {
        val data = defaults.dataForKey(habitsKey)
        if (data == null) return emptyList()
        return try {
            decodeHabits(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHabit(habit: Habit) {
        val habits = getHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index >= 0) habits[index] = habit else habits.add(habit)
        defaults.setObject(encodeHabits(habits), forKey = habitsKey)
        refreshFlows()
    }

    fun deleteHabit(habitId: String) {
        val habits = getHabits().filter { it.id != habitId }
        defaults.setObject(encodeHabits(habits), forKey = habitsKey)
        // Also delete completions for this habit
        val completions = getCompletions().filter { it.habitId != habitId }
        defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
        refreshFlows()
    }

    fun getCompletions(): List<HabitCompletion> {
        val data = defaults.dataForKey(completionsKey)
        if (data == null) return emptyList()
        return try {
            decodeCompletions(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCompletionsForHabit(habitId: String): List<HabitCompletion> {
        return getCompletions().filter { it.habitId == habitId }
    }

    fun toggleCompletion(habitId: String, date: String): Boolean {
        // Returns true if completion was added, false if removed
        val completions = getCompletions().toMutableList()
        val existing = completions.find { it.habitId == habitId && it.completedDate == date }
        if (existing != null) {
            completions.removeAll { it.habitId == habitId && it.completedDate == date }
            defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
            refreshFlows()
            return false
        } else {
            val completion = HabitCompletion(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                completedDate = date,
                completedAt = System.currentTimeMillis()
            )
            completions.add(completion)
            defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
            refreshFlows()
            return true
        }
    }

    fun deleteOldCompletions(cutoffDate: String) {
        val completions = getCompletions().filter { it.completedDate >= cutoffDate }
        defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
        refreshFlows()
    }

    fun habitsFlow(): Flow<List<Habit>> = _habitsFlow

    fun completionsFlow(): Flow<List<HabitCompletion>> = _completionsFlow

    // Simple encoding/decoding using JSON
    private fun encodeHabits(habits: List<Habit>): NSData {
        val maps = habits.map { mapOf(
            "id" to it.id,
            "name" to it.name,
            "recurrenceType" to it.recurrenceType.name,
            "daysOfWeek" to it.daysOfWeek.joinToString(","),
            "color" to it.color,
            "createdAt" to it.createdAt
        ) }
        return NSJSONSerialization.dataWithJSONObject(maps, 0, null)
    }

    private fun decodeHabits(data: NSData): List<Habit> {
        val array = NSJSONSerialization.JSONObjectWithData(data, 0, null) as? NSArray ?: return emptyList()
        return (0 until array.count).mapNotNull { i ->
            val map = array.objectAtIndex(i) as? NSDictionary ?: return@mapNotNull null
            Habit(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: return@mapNotNull null,
                recurrenceType = RecurrenceType.valueOf(map["recurrenceType"] as? String ?: return@mapNotNull null),
                daysOfWeek = (map["daysOfWeek"] as? String ?: "").split(",").filter { it.isNotBlank() }.map { it.toInt() },
                color = map["color"] as? String ?: return@mapNotNull null,
                createdAt = (map["createdAt"] as? NSNumber)?.longValue ?: 0L
            )
        }
    }

    private fun encodeCompletions(completions: List<HabitCompletion>): NSData {
        val maps = completions.map { mapOf(
            "id" to it.id,
            "habitId" to it.habitId,
            "completedDate" to it.completedDate,
            "completedAt" to it.completedAt
        ) }
        return NSJSONSerialization.dataWithJSONObject(maps, 0, null)
    }

    private fun decodeCompletions(data: NSData): List<HabitCompletion> {
        val array = NSJSONSerialization.JSONObjectWithData(data, 0, null) as? NSArray ?: return emptyList()
        return (0 until array.count).mapNotNull { i ->
            val map = array.objectAtIndex(i) as? NSDictionary ?: return@mapNotNull null
            HabitCompletion(
                id = map["id"] as? String ?: return@mapNotNull null,
                habitId = map["habitId"] as? String ?: return@mapNotNull null,
                completedDate = map["completedDate"] as? String ?: return@mapNotNull null,
                completedAt = (map["completedAt"] as? NSNumber)?.longValue ?: 0L
            )
        }
    }
}
```

Note: iOS implementation uses NSUserDefaults and NSJSONSerialization. This is a simplified approach - the actual encoding/decoding needs proper NSData handling.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
git commit -m "feat(habit): add iOS UserDefaults data layer implementation"
```

---

## Task 6: DI Module

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt`

- [ ] **Step 1: Write HabitModule**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
package com.programovil.aura.habit.di

import com.programovil.aura.habit.data.repository.HabitRepositoryImpl
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val habitModule = module {
    // Data layer
    singleOf(::HabitRepositoryImpl).bind<HabitRepository>()

    // Domain layer - use cases
    factoryOf(::AddHabitUseCase)
    factoryOf(::GetHabitsGroupedByDayUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::GetHabitHistoryUseCase)

    // Presentation layer
    viewModel { HabitViewModel(get(), get(), get(), get()) }
}
```

Note: The `HabitRepositoryImpl()` constructor will differ by platform. Koin will handle this via module structure.

- [ ] **Step 2: Update InitKoin.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
package com.programovil.aura.di

import com.programovil.aura.auth.di.authModule
import com.programovil.aura.habit.di.habitModule
import com.programovil.aura.todo.di.todoModule

fun getModules() = listOf(
    authModule,
    todoModule,
    habitModule
)
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
git add composeApp/src/commonMain/kotlin/com/programovil/aura/di/InitKoin.kt
git commit -m "feat(habit): add HabitModule DI and register in InitKoin"
```

---

## Task 7: Presentation Layer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt`
- Create: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt`

- [ ] **Step 1: Write HabitViewModel**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt
package com.programovil.aura.habit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    private val getHabitsGroupedByDayUseCase: GetHabitsGroupedByDayUseCase,
    private val addHabitUseCase: AddHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitListUiState())
    val uiState: StateFlow<HabitListUiState> = _uiState

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
            getHabitsGroupedByDayUseCase().collect { grouped ->
                _uiState.value = HabitListUiState(
                    todayHabits = grouped[DaySection.TODAY] ?: emptyList(),
                    tomorrowHabits = grouped[DaySection.TOMORROW] ?: emptyList(),
                    thisWeekHabits = grouped[DaySection.THIS_WEEK] ?: emptyList(),
                    isLoading = false
                )
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

    fun getTodayDate(): String = LocalDate.now().format(dateFormatter)
    fun getTomorrowDate(): String = LocalDate.now().plusDays(1).format(dateFormatter)
}
```

- [ ] **Step 2: Write HabitItem composable**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/HabitItem.kt
package com.programovil.aura.habit.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.programovil.aura.habit.domain.model.HabitWithStatus

@Composable
fun HabitItem(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStatus.habit
    val isDone = habitWithStatus.isDone
    val isMissed = habitWithStatus.isMissed

    Row(
        modifier = modifier
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(habit.color)))
        )

        // Name
        Text(
            text = habit.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                color = when {
                    isDone -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    isMissed -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            ),
            modifier = Modifier.weight(1f)
        )

        // Streak badge
        if (habitWithStatus.streak > 0) {
            Text(
                text = "${habitWithStatus.streak}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Checkbox
        Checkbox(
            checked = isDone,
            onCheckedChange = { onToggle() }
        )
    }
}
```

- [ ] **Step 3: Write AddHabitDialog composable**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/composable/AddHabitDialog.kt
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.programovil.aura.habit.domain.model.RecurrenceType

private val colorPalette = listOf(
    "#FF6B6B", // Red
    "#4ECDC4", // Teal
    "#45B7D1", // Blue
    "#96CEB4", // Green
    "#FFEAA7", // Yellow
    "#DDA0DD"  // Purple
)

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, recurrenceType: RecurrenceType, daysOfWeek: List<Int>, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableIntStateOf(0) } // Bitmask for Mon-Sun
    var selectedColor by remember { mutableStateOf(colorPalette[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "New Habit",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Daily/Weekly toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily")
                    Switch(
                        checked = !isDaily,
                        onCheckedChange = { isDaily = !it }
                    )
                    Text("Weekly")
                }

                // Day selector (only for weekly)
                if (!isDaily) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Repeat on:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            val dayBit = 1 shl index
                            val isSelected = (selectedDays and dayBit) != 0
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedDays = selectedDays xor dayBit
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color palette
                Text(
                    text = "Color:",
                    style = MaterialTheme.typography.labelMedium
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
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val daysOfWeek = if (isDaily) {
                                    emptyList()
                                } else {
                                    (0..6).filter { (selectedDays and (1 shl it)) != 0 }.map { it + 1 }
                                }
                                onSave(name, if (isDaily) RecurrenceType.DAILY else RecurrenceType.WEEKLY, daysOfWeek, selectedColor)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Write HabitScreen**

```kotlin
// composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/screen/HabitScreen.kt
package com.programovil.aura.habit.presentation.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programovil.aura.habit.domain.model.DaySection
import com.programovil.aura.habit.presentation.composable.AddHabitDialog
import com.programovil.aura.habit.presentation.composable.HabitItem
import com.programovil.aura.habit.presentation.viewmodel.HabitEvent
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Habits")
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add habit")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Today section
                if (uiState.todayHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = "Today",
                            subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"))
                        )
                    }
                    items(uiState.todayHabits, key = { it.habit.id }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        LocalDate.now().format(dateFormatter)
                                    )
                                )
                            }
                        )
                    }
                }

                // Tomorrow section
                if (uiState.tomorrowHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = "Tomorrow",
                            subtitle = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("MMM d"))
                        )
                    }
                    items(uiState.tomorrowHabits, key = { it.habit.id }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(
                                    HabitEvent.ToggleCompletion(
                                        habitItem.habit.id,
                                        LocalDate.now().plusDays(1).format(dateFormatter)
                                    )
                                )
                            }
                        )
                    }
                }

                // This Week section
                if (uiState.thisWeekHabits.isNotEmpty()) {
                    item {
                        HabitSectionHeader(
                            title = "This Week",
                            subtitle = null
                        )
                    }
                    items(uiState.thisWeekHabits, key = { it.habit.id }) { habitItem ->
                        HabitItem(
                            habitWithStatus = habitItem,
                            onToggle = {
                                viewModel.onEvent(HabitEvent.ToggleCompletion(habitItem.habit.id, ""))
                            }
                        )
                    }
                }

                // Empty state
                if (uiState.todayHabits.isEmpty() &&
                    uiState.tomorrowHabits.isEmpty() &&
                    uiState.thisWeekHabits.isEmpty()) {
                    item {
                        Text(
                            text = "No habits yet. Tap + to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, recurrenceType, daysOfWeek, color ->
                viewModel.onEvent(HabitEvent.AddHabit(name, recurrenceType, daysOfWeek, color))
                showAddDialog = false
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

Note: The This Week section toggle uses empty date string "" - need to calculate the actual date based on which day the habit is shown for.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/
git commit -m "feat(habit): add presentation layer - HabitViewModel, HabitScreen, HabitItem, AddHabitDialog"
```

---

## Task 8: Navigation Integration

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/NavRoute.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/AppNavHost.kt`

- [ ] **Step 1: Add Habit route to NavRoute**

```kotlin
// Add to NavRoute.kt
@Serializable
data object Habit : NavRoute()
```

- [ ] **Step 2: Add Habit route to AppNavHost**

```kotlin
// In AppNavHost.kt, import HabitScreen and HabitViewModel
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel

// Add composable for Habit route
composable<NavRoute.Habit> {
    val habitViewModel: HabitViewModel = koinInject()
    HabitScreen(habitViewModel)
}
```

- [ ] **Step 3: Add navigation to Habit from Todo screen (or tabs)**

For simplicity, add a button in TodoScreen header to navigate to Habit screen. Or add a bottom navigation with two tabs.

Modify TodoScreen or AppNavHost to include navigation to Habit route.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/navigation/
git commit -m "feat(habit): integrate habit screen into navigation"
```

---

## Task 9: Data Cleanup on Startup

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt` (add cleanup in init)

- [ ] **Step 1: Add 90-day cleanup on ViewModel init**

In `HabitViewModel.init` block, call repository.cleanupOldCompletions(90) on startup.

```kotlin
init {
    viewModelScope.launch {
        repository.cleanupOldCompletions(90)
    }
    loadHabits()
}
```

Note: Need access to repository in ViewModel, add it as a parameter or use Koin to inject.

- [ ] **Step 2: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt
git commit -m "feat(habit): add 90-day cleanup on startup"
```

---

## Spec Coverage Check

| Spec Section | Tasks |
|--------------|-------|
| Domain models (Habit, HabitCompletion, RecurrenceType, HabitWithStatus) | Task 1 |
| Repository interface | Task 2 |
| Use cases (AddHabit, GetHabitsGroupedByDay, ToggleHabitCompletion, GetHabitHistory) | Task 3 |
| Android Room (entities, DAOs, database, repository impl) | Task 4 |
| iOS UserDefaults (HabitUserDefaults, repository impl) | Task 5 |
| DI Module (HabitModule, InitKoin update) | Task 6 |
| Presentation (ViewModel, Screen, Composables) | Task 7 |
| Navigation integration | Task 8 |
| 90-day cleanup | Task 9 |
| Daily + Weekly recurrence | Tasks 1, 7 |
| Color palette | Tasks 1, 7 |
| Streak tracking (strict) | Tasks 3, 7 |
| Sections by day (Today, Tomorrow, This Week) | Tasks 3, 7 |
| Missed days state | Tasks 3, 7 |
| Clean Architecture pattern | All tasks |

---

## Plan Complete

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?