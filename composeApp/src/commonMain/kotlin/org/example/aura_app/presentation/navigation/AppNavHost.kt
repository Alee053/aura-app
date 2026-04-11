package org.example.aura_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.example.aura_app.presentation.todo.TodoScreen
import org.example.aura_app.presentation.todo.TodoViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinInject()
    NavHost(navController = navController, startDestination = NavRoute.Todo) {
        composable<NavRoute.Todo> {
            TodoScreen(todoViewModel)
        }
    }
}