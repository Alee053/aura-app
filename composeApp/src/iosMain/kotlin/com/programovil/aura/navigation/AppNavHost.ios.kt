package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    onSignOut: () -> Unit
) {
    // iOS implementation - RegionSync is Android-only
    // This would show a message that the feature is not available on iOS
}
