package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.settings.presentation.screen.SettingsScreen
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            val homeViewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                onTodoClick = { navController.navigate(NavRoute.Todo) },
                onHabitClick = { navController.navigate(NavRoute.Habit) },
                onSettingsClick = { navController.navigate(NavRoute.Settings) }
            )
        }
        composable<NavRoute.Todo> {
            TodoScreen(
                viewModel = todoViewModel
            )
        }
        composable<NavRoute.Habit> {
            HabitScreen()
        }
        composable<NavRoute.Settings> {
            val settingsViewModel = koinViewModel<SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange,
                onSignOut = onSignOut
            )
        }
    }
}
