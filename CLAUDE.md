# Architecture Specification

This project follows the Clean Architecture pattern inspired by `ucbp26`.

## Package Structure

```
com.programovil.aura/
├── App.kt
├── navigation/
│   ├── AppNavHost.kt
│   └── NavRoute.kt
├── auth/                          # Simple feature - minimal refactor
│   ├── di/
│   │   └── AuthModule.kt
│   └── presentation/
│       └── AuthViewModel.kt
├── todo/                          # Full Clean Architecture
│   ├── data/
│   │   ├── repository/
│   │   │   └── TodoRepositoryImpl.kt
│   │   └── mapper/
│   │       └── TodoMapper.kt
│   ├── domain/
│   │   ├── model/
│   │   │   └── Todo.kt
│   │   ├── repository/
│   │   │   └── TodoRepository.kt
│   │   └── usecase/
│   │       ├── AddTodoUseCase.kt
│   │       ├── DeleteTodoUseCase.kt
│   │       ├── GetTodosUseCase.kt
│   │       └── ToggleTodoUseCase.kt
│   ├── presentation/
│   │   ├── screen/
│   │   │   └── TodoScreen.kt
│   │   ├── viewmodel/
│   │   │   └── TodoViewModel.kt
│   │   └── composable/
│   │       └── TodoItem.kt
│   └── di/
│       └── TodoModule.kt
├── di/
│   ├── DataModule.kt
│   ├── DomainModule.kt
│   ├── PresentationModule.kt
│   └── InitKoin.kt
└── shared/
    └── FirebaseConfig.kt
```

## Layer Definitions

### Domain Layer (`/domain/`)
- **Models**: Pure data classes (`Todo.kt`)
- **Repository interfaces**: Abstract definitions (`TodoRepository.kt`)
- **Use cases**: Single-responsibility business logic (`GetTodosUseCase.kt`, `AddTodoUseCase.kt`, etc.)

### Data Layer (`/data/`)
- **Repository implementations**: Implements domain interfaces (`TodoRepositoryImpl.kt`)
- **Mappers**: Transforms DTOs to domain models (`TodoMapper.kt`)
- **Data sources**: (Future) Remote/local data sources

### Presentation Layer (`/presentation/`)
- **Screen**: Composables that represent full screens (`TodoScreen.kt`)
- **ViewModel**: Manages UI state and business logic coordination (`TodoViewModel.kt`)
- **Composable**: Reusable UI components (`TodoItem.kt`)

### Navigation (`/navigation/`)
- **NavRoute.kt**: Sealed class defining all navigation routes
- **AppNavHost.kt**: NavHost composable with route definitions

## DI Architecture

### Global Modules (`/di/`)
Each module focuses on registering one layer's dependencies:

- **DataModule.kt**: Registers repository implementations
- **DomainModule.kt**: Registers use cases
- **PresentationModule.kt**: Registers ViewModels
- **InitKoin.kt**: Composes all modules via `getModules()`

### Feature Modules (`/di/` within each feature)
Each feature has its own DI module for encapsulation:

- **todo/di/TodoModule.kt**: Registers `TodoRepository`, `TodoViewModel`, and use cases
- **auth/di/AuthModule.kt**: Registers `AuthViewModel`

### DI Flow
```
todo/di/TodoModule.kt
├── single<TodoRepository> { TodoRepositoryImpl(...) }
├── factory { AddTodoUseCase(get()) }
├── factory { GetTodosUseCase(get()) }
├── factory { DeleteTodoUseCase(get()) }
├── factory { ToggleTodoUseCase(get()) }
└── viewModel { TodoViewModel(...) }

Global DI imports feature modules:
di/
├── DataModule.kt      ← empty (no data-only modules yet)
├── DomainModule.kt    ← imports use cases from features
├── PresentationModule.kt ← imports ViewModels from features
└── InitKoin.kt        ← getModules() = listOf(feature modules...)
```

## Naming Conventions

- **Files**: PascalCase (`TodoViewModel.kt`, `NavRoute.kt`)
- **Use cases**: Verb-Noun pattern (`GetTodosUseCase`, `AddTodoUseCase`)
- **ViewModels**: FeatureName + ViewModel (`TodoViewModel`)
- **Screens**: FeatureName + Screen (`TodoScreen`)
- **Composables**: Descriptive name (`TodoItem`, `StarRating`)

## State Management

ViewModels expose:
- **StateFlow** for UI state
- **Events** for user actions (optional, via sealed interfaces)
- **Effects** for one-time events like navigation or snackbars (optional)

## Reference

This architecture mirrors the structure used in `ucbp26` (University course reference project).
