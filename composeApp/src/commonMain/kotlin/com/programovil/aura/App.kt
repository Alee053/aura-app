package com.programovil.aura

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import com.programovil.aura.navigation.AppNavHost
import com.programovil.aura.navigation.NavRoute
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    onSignInClick: () -> Unit = {}
) {
    MaterialTheme {
        // 🔷 Mostrar directamente RegionSync sin autenticación
        AuthenticatedApp(
            onSignOut = {}
        )
    }
}

@Composable
fun AuthenticatedApp(
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val todoViewModel: TodoViewModel = koinViewModel()
    val featureFlagManager: FeatureFlagManager = koinInject()
    val featureFlags by featureFlagManager.flags.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(Unit) {
        featureFlagManager.initialize()
    }

    val showTodos by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.TODOS_ENABLED] ?: true)
    }
    val showHabits by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.HABITS_ENABLED] ?: true)
    }
    val showNotifications by remember(featureFlags) {
        mutableStateOf(featureFlags[FeatureFlag.NOTIFICATIONS_ENABLED] ?: true)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Region Sync") },
                    label = { Text("Sync") },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute<NavRoute.RegionSync>() } == true,
                    onClick = {
                        navController.navigate(NavRoute.RegionSync) {
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

