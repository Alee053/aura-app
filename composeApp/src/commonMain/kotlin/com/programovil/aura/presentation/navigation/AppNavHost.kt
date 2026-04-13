package com.programovil.aura.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.programovil.aura.todo.presentation.TodoScreen
import com.programovil.aura.todo.presentation.TodoViewModel
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