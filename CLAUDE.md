# Architecture Specification

This project follows the Clean Architecture pattern inspired by `ucbp26`.

## Package Structure

```
com.programovil.aura/
├── App.kt
├── navigation/
│   ├── AppNavHost.kt
│   └── NavRoute.kt          # Sealed class with routes + parameterized routes
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
│       └── TodoModule.kt          # Self-contained DI for todo feature
├── di/
│   └── InitKoin.kt                # Composes all feature modules via getModules()
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
- **NavRoute.kt**: Sealed class defining all navigation routes (both `object` and `data class` routes)
- **AppNavHost.kt**: NavHost composable with route definitions, uses type-safe `toRoute<>()` for parameterized routes

## DI Architecture (Per-Feature)

Each feature is self-contained with its own DI module that registers all dependencies for that feature.

### Feature Modules

**`auth/di/AuthModule.kt`** - Simple feature:
```kotlin
val authModule = module {
    viewModel { AuthViewModel() }
}
```

**`todo/di/TodoModule.kt`** - Full Clean Architecture feature:
```kotlin
val todoModule = module {
    // Data layer
    singleOf(::TodoRepositoryImpl).bind<TodoRepository>()

    // Domain layer - use cases
    factoryOf(::GetTodosUseCase)
    factoryOf(::AddTodoUseCase)
    factoryOf(::ToggleTodoUseCase)
    factoryOf(::DeleteTodoUseCase)

    // Presentation layer
    viewModel { TodoViewModel(get(), get(), get(), get()) }
}
```

### InitKoin

**`di/InitKoin.kt`** - Composes all feature modules:
```kotlin
fun getModules() = listOf(
    authModule,
    todoModule
)
```

### Koin Patterns
- **`singleOf`** - Singleton scope (one instance across app) - for repository implementations
- **`factoryOf`** - Factory scope (new instance per injection) - **preferred for use cases**
- **`viewModel { }`** - ViewModel registration with constructor injection via `get()` - **required for ViewModels with constructor params**
- **`viewModelOf`** - ViewModel registration via constructor reference - **only works for ViewModels with no constructor params**

## Naming Conventions

- **Files**: PascalCase (`TodoViewModel.kt`, `NavRoute.kt`)
- **Use cases**: Verb-Noun pattern (`GetTodosUseCase`, `AddTodoUseCase`)
- **ViewModels**: FeatureName + ViewModel (`TodoViewModel`)
- **Screens**: FeatureName + Screen (`TodoScreen`)
- **Composables**: Descriptive name (`TodoItem`, `StarRating`)
- **Navigation routes**: `NavRoute` suffix, sealed class pattern

## Navigation Patterns

### Simple Routes (no params)
```kotlin
// NavRoute.kt
@Serializable
data object Todo : NavRoute()

// AppNavHost.kt
composable<NavRoute.Todo> {
    TodoScreen(viewModel)
}
```

### Parameterized Routes (with params)
```kotlin
// NavRoute.kt
@Serializable
data class TodoDetail(val todoId: String) : NavRoute()

// AppNavHost.kt
import androidx.navigation.toRoute

composable<NavRoute.TodoDetail> { backStackEntry ->
    val todoDetail: NavRoute.TodoDetail = backStackEntry.toRoute()
    // Use todoDetail.todoId
}
```

## State Management

ViewModels expose:
- **StateFlow** for UI state
- **Events** for user actions (optional, via sealed interfaces)
- **Effects** for one-time events like navigation or snackbars (optional)

## Reference

This architecture mirrors the structure used in `ucbp26` (University course reference project).
