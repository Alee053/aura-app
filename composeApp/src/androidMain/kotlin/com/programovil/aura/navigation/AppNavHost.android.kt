package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.programovil.aura.regionsync.presentation.RegionSyncScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    onSignOut: () -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.RegionSync) {
        composable<NavRoute.RegionSync> {
            RegionSyncScreen()
        }
    }
}
