package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.programovil.aura.habit.presentation.screen.HabitScreen
import com.programovil.aura.notification.presentation.screen.NotificationSettingsScreen
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    onSignOut: () -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.Todo) {
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
        composable<NavRoute.NotificationSettings> {
            val notificationViewModel: NotificationViewModel = koinInject()
            NotificationSettingsScreen(
                viewModel = notificationViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
