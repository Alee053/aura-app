package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.programovil.aura.regionsync.presentation.RegionSyncScreen

@Composable
actual fun AppNavHost(
    navController: NavHostController,
    todoViewModel: com.programovil.aura.todo.presentation.viewmodel.TodoViewModel,
    onSignOut: () -> Unit
) {
    NavHost(navController = navController, startDestination = NavRoute.RegionSync) {
        composable<NavRoute.RegionSync> {
            RegionSyncScreen()
        }
    }
}
