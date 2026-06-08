package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.journal.presentation.screen.JournalDetailScreen
import com.programovil.aura.journal.presentation.screen.JournalScreen
import com.programovil.aura.journal.presentation.viewmodel.JournalDetailViewModel
import com.programovil.aura.journal.presentation.viewmodel.JournalViewModel
import com.programovil.aura.pomodoro.presentation.PomodoroScreen
import com.programovil.aura.pomodoro.presentation.PomodoroViewModel
import com.programovil.aura.settings.presentation.screen.SettingsScreen
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    featureFlags: Map<FeatureFlag, Boolean>
) {
    NavHost(navController = navController, startDestination = NavRoute.Home) {
        composable<NavRoute.Home> {
            val homeViewModel = koinViewModel<HomeViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                showTodos = featureFlags[FeatureFlag.TODOS_ENABLED] != false,
                showHabits = featureFlags[FeatureFlag.HABITS_ENABLED] != false,
                onTodoClick = {
                    if (featureFlags[FeatureFlag.TODOS_ENABLED] != false) {
                        navController.navigate(NavRoute.Todo)
                    }
                },
                onHabitClick = {
                    if (featureFlags[FeatureFlag.HABITS_ENABLED] != false) {
                        navController.navigate(NavRoute.Habit)
                    }
                },
                onSettingsClick = { navController.navigate(NavRoute.Settings) }
            )
        }

        if (featureFlags[FeatureFlag.TODOS_ENABLED] != false) {
            composable<NavRoute.Todo> {
                TodoScreen(
                    viewModel = todoViewModel,
                    featureFlags = featureFlags,
                    onFeatureDisabled = {
                        navController.popBackStack(NavRoute.Home, inclusive = false)
                    }
                )
            }
        }

        if (featureFlags[FeatureFlag.HABITS_ENABLED] != false) {
            composable<NavRoute.Habit> {
                HabitScreen(
                    featureFlags = featureFlags,
                    onFeatureDisabled = {
                        navController.popBackStack(NavRoute.Home, inclusive = false)
                    }
                )
            }
        }

        if (featureFlags[FeatureFlag.JOURNAL_ENABLED] != false) {
            composable<NavRoute.Journal> {
                val journalViewModel = koinViewModel<JournalViewModel>()
                JournalScreen(
                    viewModel = journalViewModel,
                    onNavigateToDetail = { entryId ->
                        navController.navigate(NavRoute.JournalDetail(entryId = entryId))
                    },
                    featureFlags = featureFlags,
                    onFeatureDisabled = {
                        navController.popBackStack(NavRoute.Home, inclusive = false)
                    }
                )
            }

            composable<NavRoute.JournalDetail> { backStackEntry ->
                val entryId = backStackEntry.toRoute<NavRoute.JournalDetail>().entryId
                val detailViewModel = koinViewModel<JournalDetailViewModel>(
                    parameters = { parametersOf(entryId) }
                )
                val journalViewModel = koinViewModel<JournalViewModel>()
                JournalDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDelete = {
                        detailViewModel.uiState.value.entry?.let { entry ->
                            journalViewModel.deleteEntry(entry)
                        }
                        navController.popBackStack()
                    }
                )
            }
        }

        if (featureFlags[FeatureFlag.POMODORO_ENABLED] != false) {
            composable<NavRoute.Pomodoro> {
                val pomodoroViewModel = koinViewModel<PomodoroViewModel>()
                PomodoroScreen(
                    viewModel = pomodoroViewModel,
                    featureFlags = featureFlags,
                    onFeatureDisabled = {
                        navController.popBackStack(NavRoute.Home, inclusive = false)
                    },
                    onSettingsClick = { navController.navigate(NavRoute.Settings) }
                )
            }
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
