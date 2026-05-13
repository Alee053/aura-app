package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.FocusScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.screen.ProgressScreen
import com.programovil.aura.settings.presentation.screen.SettingsScreen
import com.programovil.aura.home.presentation.screen.TasksScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.home.presentation.viewmodel.FocusViewModel
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel
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
                onTasksClick = { navController.navigate(NavRoute.Tasks) },
                onFocusClick = { navController.navigate(NavRoute.Focus) },
                onProgressClick = { navController.navigate(NavRoute.Progress) },
                onSettingsClick = { navController.navigate(NavRoute.Settings) }
            )
        }
        composable<NavRoute.Tasks> {
            TasksScreen(
                todoViewModel = todoViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Focus> {
            val focusViewModel = koinViewModel<FocusViewModel>()
            FocusScreen(
                viewModel = focusViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Progress> {
            val progressViewModel = koinViewModel<ProgressViewModel>()
            ProgressScreen(
                viewModel = progressViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Settings> {
            val settingsViewModel = koinViewModel<SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                currentThemeMode = currentThemeMode,
                onThemeChange = onThemeChange,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.Todo> {
            TodoScreen(
                viewModel = todoViewModel,
                onSignOut = onSignOut
            )
        }
        composable<NavRoute.TodoDetail> { backStackEntry ->
            val todoDetail: NavRoute.TodoDetail = backStackEntry.toRoute()
            // TODO: Navigate to detail screen with todoDetail.todoId
        }
        composable<NavRoute.Habit> {
            HabitScreen(
                onSignOut = onSignOut
            )
        }
    }
}
