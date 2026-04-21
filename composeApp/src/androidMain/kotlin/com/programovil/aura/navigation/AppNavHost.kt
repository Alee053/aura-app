package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.programovil.aura.notification.presentation.screen.NotificationSettingsScreen
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost(
    onNavigateToSettings: () -> Unit
) {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinInject()
    val notificationViewModel: NotificationViewModel = koinInject()

    NavHost(navController = navController, startDestination = NavRoute.Todo) {
        composable<NavRoute.Todo> {
            TodoScreen(
                viewModel = todoViewModel,
                onSettingsClick = {
                    navController.navigate(NavRoute.NotificationSettings)
                }
            )
        }
        composable<NavRoute.NotificationSettings> {
            NotificationSettingsScreen(
                viewModel = notificationViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<NavRoute.TodoDetail> { backStackEntry ->
            val todoDetail: NavRoute.TodoDetail = backStackEntry.toRoute()
        }
    }
}