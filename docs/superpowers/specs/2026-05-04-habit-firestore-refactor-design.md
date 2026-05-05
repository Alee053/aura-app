# Habit Feature: Room-to-Firestore Refactor Design

**Date:** 2026-05-04  
**Status:** Approved  
**Approach:** A — Mirror the Todo Pattern Exactly

---

## 1. Goal

Completely remove the Room KMP local database from the `habit` feature and replace it with a fully online Firestore implementation, consistent with how the `todo` feature is already implemented.

---

## 2. Context

The `habit` feature currently persists data via Room KMP:
- `HabitEntity` + `HabitCompletionEntity` (Room entities)
- `HabitDao` + `HabitCompletionDao` (Room DAOs)
- `HabitDatabase` with expect/actual builders for Android and iOS
- `HabitRepositoryImpl` directly queries the Room database

The `todo` feature is already fully Firestore-based:
- `TodoRepository` interface in `commonMain` with `expect fun createTodoRepository()`
- Android actual implements Firestore via `FirebaseConfig.firestore`
- iOS actual returns no-op stubs
- No local database at all

This refactor makes `habit` follow the exact same pattern as `todo`.

---

## 3. Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| iOS implementation | Stubs / no-op | Matches the existing todo pattern. Fastest path to consistency. |
| Repository interface pattern | `Flow<Result<...>>` | Aligns with todo. Errors flow naturally through the stream. |
| Old data cleanup | Remove entirely | Firestore stores data online; no client-side cleanup needed. |
| Approach | A — Mirror Todo | Simplest, most consistent, removes the most code. |

---

## 4. Architecture & File Changes

### 4.1 Files to Delete

All Room-related files are removed:

```
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitEntity.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/entity/HabitCompletionEntity.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitDao.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitCompletionDao.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitMapper.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.android.kt
composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/local/HabitDatabase.ios.kt
```

### 4.2 Files to Create

```
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/data/mapper/HabitData.kt
composeApp/src/androidMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
composeApp/src/iosMain/kotlin/com/programovil/aura/habit/data/repository/HabitRepositoryImpl.kt
```

### 4.3 Files to Modify

```
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/repository/HabitRepository.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/di/HabitModule.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitsGroupedByDayUseCase.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/domain/usecase/GetHabitHistoryUseCase.kt
composeApp/src/commonMain/kotlin/com/programovil/aura/habit/presentation/viewmodel/HabitViewModel.kt
composeApp/build.gradle.kts
gradle/libs.versions.toml
```

---

## 5. Firestore Schema

Collections are scoped per user, identical to todo:

```
/users/{userId}/habits/{habitId}
/users/{userId}/completions/{completionId}
```

### 5.1 Habit Document

| Field | Type | Notes |
|-------|------|-------|
| `name` | String | Habit title |
| `recurrenceType` | String | `"DAILY"` or `"WEEKLY"` |
| `daysOfWeek` | List<Int> | Empty for `DAILY`; e.g. `[1, 3, 5]` for Mon/Wed/Fri |
| `color` | String | Hex color code |
| `createdAt` | Timestamp | `FieldValue.serverTimestamp()` |

### 5.2 Completion Document

| Field | Type | Notes |
|-------|------|-------|
| `habitId` | String | Reference to the habit |
| `completedDate` | String | `YYYY-MM-DD` format |
| `completedAt` | Timestamp | `FieldValue.serverTimestamp()` |

---

## 6. Data Flow

### 6.1 Reading Habits

`getHabits()` uses Firestore `addSnapshotListener` inside `callbackFlow`:
1. Listen to `/users/{userId}/habits`
2. Map each document to `HabitData` DTO
3. Convert DTO to domain `Habit`
4. Emit `Result.success(list)` or `Result.failure(error)`

### 6.2 Reading Completions

`getAllCompletions()` uses the same pattern on `/users/{userId}/completions`.

`getCompletionsForHabit(habitId)` listens to `/users/{userId}/completions` with a Firestore query `.whereEqualTo("habitId", habitId)`.

### 6.3 Writing Habits

`addHabit(habit)` calls `.add()` on the habits collection with the document map.

### 6.4 Toggling Completions

`toggleCompletion(habitId, date)`:
1. Query completions for `habitId` + `completedDate == date`
2. If a document exists → `.delete()` it (untoggle)
3. If no document exists → `.add()` a new completion (toggle on)

### 6.5 Grouped Use Case

`GetHabitsGroupedByDayUseCase` combines:
- `repository.getHabits(): Flow<Result<List<Habit>>>`
- `repository.getAllCompletions(): Flow<Result<List<HabitCompletion>>>`

It unwraps both `Result` wrappers. If both are success, it builds `Map<DaySection, List<HabitWithStatus>>` and emits `Result.success(map)`. If either is failure, it emits `Result.failure(error)`.

---

## 7. Domain & Presentation Changes

### 7.1 Repository Interface

```kotlin
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

**Removed:** `cleanupOldCompletions(olderThanDays: Int)` — no longer needed.

### 7.2 Use Cases

| Use Case | Change |
|----------|--------|
| `AddHabitUseCase` | No signature change. Still returns `Result<Unit>`. |
| `ToggleHabitCompletionUseCase` | No signature change. Still returns `Result<Unit>`. |
| `GetHabitHistoryUseCase` | Return type changes from `Flow<List<HabitCompletion>>` to `Flow<Result<List<HabitCompletion>>>`. |
| `GetHabitsGroupedByDayUseCase` | Return type changes from `Flow<Map<DaySection, List<HabitWithStatus>>>` to `Flow<Result<Map<DaySection, List<HabitWithStatus>>>>`. Internally unwraps `Result` from both combined flows. |

### 7.3 ViewModel

- **Remove** `cleanupOldCompletions(90)` call from `init`
- `loadHabits()`: Collect from `GetHabitsGroupedByDayUseCase` and handle `Result`:
  - `Result.success` → update UI state with grouped habits
  - `Result.failure` → set `error` message in UI state
- Constructor injection stays unchanged (Koin resolves dependencies)

---

## 8. Build System Changes

Since Room is **only** used by the habit feature, all Room references can be safely removed.

### 8.1 `composeApp/build.gradle.kts`

**Remove:**
- `alias(libs.plugins.room)`
- `implementation(libs.room.runtime)`
- `room { ... }` block
- KSP compiler lines for Room

### 8.2 `gradle/libs.versions.toml`

**Remove:**
- `room = "2.8.4"` version
- `room-runtime`, `room-ktx`, `room-compiler` library entries
- `room = { id = "androidx.room", ... }` plugin entry

---

## 9. iOS Stub Implementation

The iOS actual matches the todo stub pattern:

```kotlin
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

---

## 10. Testing Considerations

- The project currently does not appear to have unit tests for the repository layer.
- After the refactor, the repository implementation is platform-specific (Android = Firestore, iOS = stubs).
- No new test files are required as part of this refactor unless the user explicitly requests them.
- Manual verification path: run the Android app, add habits, toggle completions, verify Firestore documents appear in the Firebase console.

---

## 11. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| `GetHabitsGroupedByDayUseCase` combine logic breaks with `Result` wrappers | Carefully unwrap both Results; emit failure if either fails. |
| Firestore offline behavior differs from Room | Firestore has built-in offline persistence by default on Android; behavior should be acceptable. |
| iOS stubs cause empty UI | Matches existing todo behavior; expected and accepted per decision. |
| Build breaks after removing Room | Verify no other module uses Room before deleting from `libs.versions.toml`. |

---

## 12. Post-Refactor Package Structure

```
habit/
├── data/
│   └── mapper/
│       └── HabitData.kt          # DTOs for Firestore mapping
├── domain/
│   ├── model/
│   │   ├── DaySection.kt
│   │   ├── Habit.kt
│   │   ├── HabitCompletion.kt
│   │   ├── HabitWithStatus.kt
│   │   └── RecurrenceType.kt
│   ├── repository/
│   │   └── HabitRepository.kt    # Interface + expect factory
│   └── usecase/
│       ├── AddHabitUseCase.kt
│       ├── GetHabitHistoryUseCase.kt
│       ├── GetHabitsGroupedByDayUseCase.kt
│       └── ToggleHabitCompletionUseCase.kt
├── presentation/
│   ├── composable/
│   │   ├── AddHabitDialog.kt
│   │   └── HabitItem.kt
│   ├── screen/
│   │   └── HabitScreen.kt
│   └── viewmodel/
│       └── HabitViewModel.kt
└── di/
    └── HabitModule.kt
```

Platform actuals live in:
```
androidMain/.../habit/data/repository/HabitRepositoryImpl.kt
iosMain/.../habit/data/repository/HabitRepositoryImpl.kt
```
