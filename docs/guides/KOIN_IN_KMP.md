# Koin Dependency Injection in Kotlin Multiplatform

This guide covers setting up Koin for a Kotlin Multiplatform (KMP) project with Compose Multiplatform.

## 1. Version Catalog (`libs.versions.toml`)

```toml
[versions]
koin = "4.1.1"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
```

## 2. Dependency Declaration (`composeApp/build.gradle.kts`)

```kotlin
sourceSets {
    androidMain.dependencies {
        implementation(libs.koin.android)
    }
    commonMain.dependencies {
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)
    }
}
```

## 3. Android Application (`androidMain`)

Initialize Koin in the Android `Application` class:

```kotlin
class AndroidApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules())
        }
    }
}
```

Wire it in `AndroidManifest.xml`:
```xml
<application
    android:allowBackup="true"
    android:name=".AndroidApp"
    ...>
```

## 4. Koin Patterns

### Module organization
Each feature defines its own Koin module:

```kotlin
// commonMain
val presentationModule = module {
    viewModelOf(::MyViewModel)
}

val domainModule = module {
    factoryOf(::MyUseCase)
}

val dataModule = module {
    singleOf(::MyRepositoryImpl) bind MyRepository::class
}
```

### Using ViewModels in Composables

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
    // ...
}
```

### Using injected dependencies in Composables

```kotlin
@Composable
fun MyScreen() {
    val service: MyService = koinInject()
}
```

### Root module composition

```kotlin
fun getModules() = listOf(
    domainModule,
    presentationModule,
    dataModule
)
```

## 5. Best Practices

- **Per-feature modules**: Each feature owns its DI module
- **`singleOf`/`factoryOf`**: Preferred for automatic constructor injection
- **`viewModelOf`**: For shared ViewModels
- **`bind<Interface>()`**: Link implementations to domain interfaces
- **Platform-specific modules**: Define in `androidMain`/`iosMain` for platform-aware bindings
