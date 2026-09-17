package com.programovil.aura.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import aura_app.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.programovil.aura.shared.presentation.composable.LocalAuraAnnounce
import com.programovil.aura.shared.presentation.composable.navigateAuraTab
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.home.presentation.screen.HomeScreen
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.journal.presentation.screen.*
import com.programovil.aura.journal.presentation.viewmodel.*
import com.programovil.aura.pomodoro.presentation.*
import com.programovil.aura.settings.presentation.screen.SettingsScreen
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
private fun AccessGuard(allowed: Boolean, nav: NavHostController, content: @Composable () -> Unit) {
    val announce = LocalAuraAnnounce.current
    val unavailable = stringResource(Res.string.rd_feature_hidden)
    if (allowed) content()
    else LaunchedEffect(Unit) {
        announce(unavailable)
        nav.navigate(NavRoute.Home) { popUpTo<NavRoute.Home> { inclusive = false; saveState = true }; launchSingleTop = true }
    }
}
@Composable
fun AppNavHost(
    navController: NavHostController, todoViewModel: TodoViewModel, pomodoroViewModel: PomodoroViewModel,
    currentThemeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit, onSignOut: () -> Unit,
    featureFlags: Map<FeatureFlag, Boolean>, showHabitsAccessible: Boolean = true
) {
    val motion = LocalAuraMotion.current
    val todo = featureFlags[FeatureFlag.TODOS_ENABLED] != false
    val journal = featureFlags[FeatureFlag.JOURNAL_ENABLED] != false
    val focus = featureFlags[FeatureFlag.POMODORO_ENABLED] != false
    NavHost(navController, startDestination = NavRoute.Home,
        enterTransition = { fadeIn(tween(motion.interaction)) },
        exitTransition = { fadeOut(tween(motion.interaction)) }) {
        composable<NavRoute.Home> {
            HomeScreen(koinViewModel<HomeViewModel>(), todo, showHabitsAccessible,
                onTodoClick = { if (todo) navController.navigateAuraTab(NavRoute.Todo) },
                onHabitClick = { if (showHabitsAccessible) navController.navigateAuraTab(NavRoute.Habit) },
                onSettingsClick = { navController.navigate(NavRoute.Settings) },
                showPomodoro = focus, showJournal = journal,
                onFocusClick = { if (focus) navController.navigateAuraTab(NavRoute.Pomodoro) },
                onJournalClick = { if (journal) navController.navigateAuraTab(NavRoute.Journal) })
        }
        composable<NavRoute.Todo> {
            AccessGuard(todo, navController) { TodoScreen(todoViewModel, featureFlags) }
        }
        composable<NavRoute.Habit> {
            AccessGuard(showHabitsAccessible, navController) { HabitScreen(featureFlags) }
        }
        composable<NavRoute.Pomodoro> {
            AccessGuard(focus, navController) { PomodoroScreen(pomodoroViewModel, featureFlags) }
        }
        composable<NavRoute.Journal> {
            AccessGuard(journal, navController) {
                JournalScreen(koinViewModel<JournalViewModel>(),
                    { id -> navController.navigate(NavRoute.JournalDetail(id)) }, featureFlags)
            }
        }
        composable<NavRoute.JournalDetail>(
            enterTransition = { fadeIn(tween(motion.navigation)) + slideInHorizontally(tween(motion.navigation)) { it / 12 } },
            popExitTransition = { fadeOut(tween(motion.state)) + slideOutHorizontally(tween(motion.state)) { it / 12 } }
        ) { entry ->
            AccessGuard(journal, navController) {
                val id = entry.toRoute<NavRoute.JournalDetail>().entryId
                val vm = koinViewModel<JournalDetailViewModel>(parameters = { parametersOf(id) })
                JournalDetailScreen(vm, { navController.popBackStack() })
            }
        }
        composable<NavRoute.Settings>(
            enterTransition = { fadeIn(tween(motion.navigation)) + slideInHorizontally(tween(motion.navigation)) { it / 12 } },
            popExitTransition = { fadeOut(tween(motion.state)) }
        ) {
            SettingsScreen(koinViewModel<SettingsViewModel>(), currentThemeMode, onThemeChange, onSignOut,
                onNavigateBack = { navController.popBackStack() })
        }
    }
}
