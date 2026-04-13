package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinInject()
    NavHost(navController = navController, startDestination = NavRoute.Todo) {
        composable<NavRoute.Todo> {
            TodoScreen(todoViewModel)
        }
        composable<NavRoute.TodoDetail> { backStackEntry ->
            val todoDetail: NavRoute.TodoDetail = backStackEntry.toRoute()
            // TODO: Navigate to detail screen with todoDetail.todoId
        }
    }
}