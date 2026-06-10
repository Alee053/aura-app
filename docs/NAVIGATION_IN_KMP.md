# Navigation in KMP with Compose Multiplatform

Type-safe navigation using `androidx-navigation` Compose and `kotlinx-serialization`.

## 1. Dependencies (`libs.versions.toml`)

```toml
[versions]
navigationCompose = "2.9.2"
kotlinxSerializationJson = "1.8.0"

[libraries]
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

## 2. Build Configuration

```kotlin
// composeApp/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinSerialization)
}

// commonMain.dependencies
implementation(libs.navigation.compose)
implementation(libs.kotlinx.serialization.json)
```

## 3. Route Definitions

Use a `@Serializable` sealed class for type-safe routes:

```kotlin
import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Home : NavRoute()

    @Serializable
    data object Profile : NavRoute()

    @Serializable
    data class Detail(val id: String) : NavRoute()
}
```

- **Object routes**: For parameterless destinations
- **Data class routes**: For destinations with typed parameters

## 4. NavHost Setup

```kotlin
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            HomeScreen(navController)
        }
        composable<NavRoute.Profile> {
            ProfileScreen(navController)
        }
        composable<NavRoute.Detail> { backStackEntry ->
            val route = backStackEntry.toRoute<NavRoute.Detail>()
            DetailScreen(navController, id = route.id)
        }
    }
}
```

## 5. Navigating with ViewModel Effects

ViewModel emits navigation effects via a channel/flow:

```kotlin
@Composable
fun ProfileScreen(navController: NavHostController) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateToEdit -> navController.navigate(NavRoute.ProfileEdit)
            }
        }
    }
}
```
