package com.programovil.aura.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel

@Composable
expect fun AppNavHost(
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    onSignOut: () -> Unit
)
