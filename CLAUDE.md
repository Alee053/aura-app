# Architecture Specification

This project follows a strict Clean Architecture pattern optimized for Kotlin Multiplatform (KMP), inspired by `ucbp26`.

## Package Structure

```
com.programovil.aura/
├── App.kt                          # Root entry point with Bottom Navigation
├── di/
│   └── InitKoin.kt                # Shared Koin initialization for all features
├── navigation/
│   ├── AppNavHost.kt              # Shared NavHost with cross-platform routes
│   └── NavRoute.kt                # Type-safe routes via kotlinx-serialization
├── auth/                          # Auth feature - abstracted for KMP
│   ├── domain/AuthService.kt      # Interface for platform-specific auth
│   ├── presentation/              # AuthViewModel (Shared logic)
│   └── di/AuthModule.kt
├── todo/                          # Todo feature - Firestore backed
│   ├── domain/                    # Models, Repository Interface, UseCases
│   ├── data/repository/           # Platform-specific Repository (expect/actual)
│   ├── presentation/              # Screen, ViewModel, Composable
│   └── di/TodoModule.kt
├── habit/                         # Habit feature - Room KMP backed
│   ├── domain/                    # Models, Repository Interface, UseCases
│   ├── data/                      # Shared Repository, DAOs, Entities
│   │   ├── local/HabitDatabase.kt # Shared DB definition & expect builder
│   │   └── repository/            # Shared Repository implementation
│   ├── presentation/              # Screen, ViewModel, Composable
│   └── di/HabitModule.kt
├── notification/                  # Notification feature
│   ├── domain/NotificationScheduler.kt # Interface for scheduling
│   ├── presentation/              # ViewModel, Settings Screen
│   └── di/NotificationModule.kt
└── shared/                        # Cross-cutting concerns
    ├── data/DataStoreFactory.kt   # Shared KMP DataStore
    └── ColorUtils.kt              # KMP Color parsing utilities
```

## Layer Definitions

### Domain Layer (`/domain/`)
- **Models**: Pure data classes (e.g., `Habit.kt`).
- **Interfaces**: Service or Repository definitions (e.g., `AuthService.kt`, `HabitRepository.kt`).
- **Use cases**: Single-responsibility logic (e.g., `GetHabitsGroupedByDayUseCase.kt`).

### Data Layer (`/data/`)
- **Repository implementations**: Concrete implementations of domain interfaces.
- **Local Source**: Room KMP (`HabitDatabase`), platform `actual` builders.
- **Remote Source**: Firebase Firestore (implemented via platform actuals).
- **Mappers**: DTO/Entity to Domain model transformations.

### Presentation Layer (`/presentation/`)
- **Screens**: Composables representing full navigation destinations.
- **ViewModels**: Manage UI state (Shared logic).
- **Composables**: Reusable UI units (e.g., `HabitItem.kt`).

## DI Architecture (Per-Feature)

Features are self-contained. Their modules are composed in `di/InitKoin.kt`.

### Koin Patterns
- **`singleOf` / `factoryOf`**: Preferred for automatic constructor injection.
- **`viewModelOf`**: For shared ViewModels.
- **`bind<Interface>()`**: To link implementations to domain interfaces.

## Persistence Patterns

### Local Persistence (Room KMP)
Defined in `commonMain` with `expect fun getHabitDatabaseBuilder()`.
- **Android**: Stores in app database path using `AndroidSQLiteDriver`.
- **iOS**: Stores in `NSHomeDirectory` using `BundledSQLiteDriver`.

### Cloud Persistence (Firestore)
Abstracted behind repository interfaces to handle platform-specific SDK behavior.

## Naming Conventions
- **Files**: PascalCase.
- **Use Cases**: Verb-Noun pattern (`AddHabitUseCase`).
- **ViewModels**: `FeatureName + ViewModel`.
- **Date Handling**: Strictly use `kotlinx-datetime`.
- **UUID**: Use custom KMP-compliant random string generators.

## Navigation
Type-safe navigation using `androidx-navigation`.
- **Object Routes**: For simple destinations.
- **Data Class Routes**: For destinations with parameters.
