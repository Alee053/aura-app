package com.programovil.aura

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.auth.presentation.screen.SignInScreen
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.DsTheme
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.navigation.AppNavHost
import com.programovil.aura.navigation.NavRoute
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    onSignInClick: () -> Unit = {}
) {
    val settingsViewModel = koinViewModel<com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel>()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val currentThemeMode = settingsState.themeMode

    DsTheme(mode = currentThemeMode) {
        val authViewModel: AuthViewModel = koinViewModel()
        val authState by authViewModel.authState.collectAsState()

        when (val state = authState) {
            is AuthViewModel.AuthState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.colors.primary)
                }
            }
            is AuthViewModel.AuthState.SignedIn -> {
                AuthenticatedApp(
                    currentThemeMode = currentThemeMode,
                    onThemeChange = { settingsViewModel.setThemeMode(it) }
                )
            }
            is AuthViewModel.AuthState.SignedOut,
            is AuthViewModel.AuthState.Error -> {
                SignInScreen(
                    errorMessage = if (state is AuthViewModel.AuthState.Error) state.message else null,
                    onSignInClick = onSignInClick
                )
            }
        }
    }
}

@Composable
fun AuthenticatedApp(
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinViewModel()
    val featureFlagManager: FeatureFlagManager = koinInject()
    val featureFlags by featureFlagManager.flags.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(Unit) {
        featureFlagManager.initialize()
    }

    val showTodos by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.TODOS_ENABLED] ?: true)
    }
    val showHabits by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.HABITS_ENABLED] ?: true)
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = AppTheme.colors.primary,
        selectedTextColor = AppTheme.colors.primary,
        unselectedIconColor = AppTheme.colors.textSecondary,
        unselectedTextColor = AppTheme.colors.textSecondary,
        indicatorColor = AppTheme.colors.primary.copy(alpha = 0.15f)
    )

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.primary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Home>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    colors = navItemColors
                )
                if (showTodos) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Checklist, contentDescription = "Todos") },
                        label = { Text("Todos") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Todo>() } == true,
                        onClick = {
                            navController.navigate(NavRoute.Todo) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        colors = navItemColors
                    )
                }
                if (showHabits) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Habits") },
                        label = { Text("Habits") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Habit>() } == true,
                        onClick = {
                            navController.navigate(NavRoute.Habit) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        colors = navItemColors
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Settings>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.Settings) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    colors = navItemColors
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AppNavHost(
                navController = navController,
                todoViewModel = todoViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange
            )
        }
    }
}


