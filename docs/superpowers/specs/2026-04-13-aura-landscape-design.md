# Aura - Productivity App

## Vision

A Kotlin Multiplatform productivity app targeting Android and iOS, with **4 independent modules**: Todo, Habits, Pomodoro, and Agenda. Online-first with Firestore persistence for resilience.

---

## Current State

### Implemented
- **Auth**: Google Sign-In via Firebase Auth ✅
- **Todo module**: Full Clean Architecture (domain/data/presentation), Firestore persistence ✅
- **Shared infrastructure**: FirebaseConfig, KOIN DI setup, Navigation ✅

### Tech Stack
| Layer | Technology |
|---|---|
| Framework | Kotlin Multiplatform (Android + iOS) |
| UI | Jetpack Compose Multiplatform |
| Architecture | Clean Architecture (Domain/Data/Presentation) + MVVM |
| DI | KOIN |
| Remote DB | Cloud Firestore (online-first, native offline persistence) |
| Auth | Firebase Auth (Google Sign-In) |
| Navigation | Navigation Compose + kotlinx-serialization |

---

## Module Architecture

Each module follows Clean Architecture:

```
module/
├── data/
│   ├── repository/    # Firestore implementation
│   └── mapper/        # DTO → Domain model
├── domain/
│   ├── model/         # Pure data class
│   ├── repository/    # Interface
│   └── usecase/       # Business logic
├── presentation/
│   ├── screen/        # Full screen composable
│   ├── viewmodel/     # UI state + logic
│   └── composable/    # Reusable UI components
└── di/
    └── Module.kt      # KOIN module
```

**Firestore path pattern** (per-user, no conflicts):
```
/users/{uid}/todos/{docId}
/users/{uid}/habits/{docId}
/users/{uid}/pomodoros/{docId}
/users/{uid}/agenda/{docId}
```

---

## Decisions Made

- **Module independence**: Option A — Shared infrastructure (DI, Nav, Auth, Firestore), no cross-module dependencies except potentially Agenda → Todo
- **Offline sync**: Firestore native offline persistence, last-write-wins (built-in)
- **Auth**: Google Sign-In only (email link not needed for MVP)
- **Data isolation**: Per-user documents, no multi-user collaboration
- **Conflict strategy**: Last-write-wins (Firestore default)

---

## Upcoming Modules

### Habits
- Domain: habit entity + completion tracking
- Firestore path: `/users/{uid}/habits`
- Potential complexity: recurrence, streak logic

### Pomodoro
- Domain: simple timer (no Firestore needed for the timer itself)
- Decision pending: does pomodoro history need Firestore persistence?
- If just a timer → local-only ViewModel, no repository needed

### Agenda
- Domain: calendar events / scheduling
- Potential cross-module link: references Todo items (decision pending on how to link)

---

## Potential Concerns / Future Decisions

### 1. Pomodoro module — local vs cloud
If Pomodoro is just a timer with no history persistence needed, it could be the simplest module: a ViewModel with a `timerState` flow, no Firestore repository, no domain layer. Need to confirm intent when the time comes.

### 2. Habits data model complexity
When habits are implemented, decisions needed:
- How to track completions? (subcollection per habit `/habits/{id}/completions` or array in document?)
- Recurrence logic: daily/weekly/etc. — client-side computation or Firestore queries?
- Streak calculation: computed on read or stored/updated on write?

### 3. Agenda → Todo relationship
When Agenda is built, it may need to reference Todo items. Options:
- **A**: Agenda stores only `todoId` (loose coupling, Todo fetched on demand)
- **B**: Agenda stores denormalized `todoTitle` copy (no dependency, but stale risk)
- **C**: Shared model / cross-module use case (tighter coupling — avoid for now)

### 4. Module growth DI management
Adding Habits, Pomodoro, Agenda modules means `InitKoin.kt` composes 5 modules:
```kotlin
fun getModules() = listOf(
    authModule,
    todoModule,
    habitsModule,    // future
    pomodoroModule, // future
    agendaModule    // future
)
```
This is fine — just keep the pattern consistent.

### 5. Error handling consistency
Current repos return `Result<T>`, but there's no UI layer for error states. When building each module's ViewModel, decide:
- Does UI show error snackbars? Toasts? Inline messages?
- Does ViewModel expose `UiState` with `isError`, `errorMessage` fields?
- Or use sealed class states like `AuthState` (Loading/SignedIn/SignedOut/Error)?

### 6. ViewModel pattern — StateFlow vs MVI
Current ViewModels use simple `StateFlow`. As modules grow, consider whether MVI (single state object + events + effects) is worth adopting. Not a problem for MVP, but worth revisiting when state gets complex (e.g., Agenda with multiple event types).

### 7. Test strategy
No tests exist yet. The tech-stack doc mentions unit tests for domain/data layers and UI tests. When each module is built, follow TDD — write use case tests, repository tests, then implement.

### 8. Version catalog
Tech-stack doc says use `libs.versions.toml` but current project may not have one. Verify and enforce if Gradle setup allows it.

### 9. Auth state restoration
`AuthViewModel` checks `FirebaseConfig.auth.currentUser` on init. This works for cold starts but ensure the same logic handles:
- App backgrounded/foregrounded (auth state shouldn't change)
- Token refresh (Firebase handles automatically)

### 10. Firestore security rules
Not implemented yet. For MVP with single-user data, rules should enforce:
```javascript
match /users/{uid} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```
This is critical before any production release.

---

## Pending Decisions (for when each module is built)

| Module | Pending Questions |
|---|---|
| **Habits** | Completion tracking strategy? Recurrence client or server-side? |
| **Pomodoro** | Need history persistence? Link to specific Todo? |
| **Agenda** | How to reference Todos? Weekly/daily view priority? |

---

## File Structure (current)

```
com.programovil.aura/
├── App.kt
├── auth/
│   ├── di/AuthModule.kt
│   └── presentation/AuthViewModel.kt
├── di/InitKoin.kt
├── navigation/
│   ├── AppNavHost.kt
│   └── NavRoute.kt
├── shared/FirebaseConfig.kt
└── todo/
    ├── data/
    │   ├── mapper/TodoMapper.kt
    │   └── repository/TodoRepositoryImpl.kt
    ├── di/TodoModule.kt
    ├── domain/
    │   ├── model/Todo.kt
    │   ├── repository/TodoRepository.kt
    │   └── usecase/ (GetTodos, Add, Toggle, Delete)
    └── presentation/
        ├── composable/TodoItem.kt
        ├── screen/TodoScreen.kt
        └── viewmodel/TodoViewModel.kt
```

## Reference

- Architecture mirrors `ucbp26` project patterns
- Firestore path: `/users/{uid}/{module}/{docId}`
- KOIN patterns: `singleOf` for repos, `factoryOf` for use cases, `viewModel { }` for ViewModels