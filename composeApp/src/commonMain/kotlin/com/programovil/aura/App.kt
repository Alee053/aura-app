package com.programovil.aura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.navigation.AppNavHost
import com.programovil.aura.navigation.NavRoute
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    onSignInClick: () -> Unit = {}
) {
    MaterialTheme {
        val authViewModel: AuthViewModel = koinViewModel()
        val authState by authViewModel.authState.collectAsState()

        when (val state = authState) {
            is AuthViewModel.AuthState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthViewModel.AuthState.SignedIn -> {
                AuthenticatedApp(
                    onSignOut = { authViewModel.signOut() }
                )
            }
            is AuthViewModel.AuthState.SignedOut,
            is AuthViewModel.AuthState.Error -> {
                SignInScreen(
                    errorMessage = if (state is AuthViewModel.AuthState.Error) state.message else null,
                    onSignInClick = onSignInClick
                )
            }
        }
    }
}

@Composable
fun AuthenticatedApp(
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Todos") },
                    label = { Text("Todos") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Todo>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.Todo) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Habits") },
                    label = { Text("Habits") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.Habit>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.Habit) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AppNavHost(
                navController = navController,
                todoViewModel = todoViewModel,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
fun SignInScreen(
    errorMessage: String?,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Aura",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sign in to sync your todos",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onSignInClick) {
            Text("Sign in with Google")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
