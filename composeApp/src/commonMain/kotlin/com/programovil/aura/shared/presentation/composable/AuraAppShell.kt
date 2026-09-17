package com.programovil.aura.shared.presentation.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.navigation.NavRoute
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch

val LocalAuraGutter = staticCompositionLocalOf { 24.dp }
val LocalAuraAnnounce = staticCompositionLocalOf<(String) -> Unit> { {} }
fun NavHostController.navigateAuraTab(route: NavRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
private data class Destination(val route: NavRoute, val title: String, val icon: ImageVector)

@Composable
fun AuraAppShell(navController: NavHostController, todos: Boolean, habits: Boolean,
    pomodoro: Boolean, journal: Boolean, content: @Composable () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination
    val secondary = current?.hasRoute<NavRoute.Settings>() == true || current?.hasRoute<NavRoute.JournalDetail>() == true
    val destinations = buildList {
        add(Destination(NavRoute.Home, stringResource(Res.string.nav_home), Icons.Outlined.Home))
        if (todos) add(Destination(NavRoute.Todo, stringResource(Res.string.nav_todos), Icons.Outlined.Checklist))
        if (habits) add(Destination(NavRoute.Habit, stringResource(Res.string.nav_habits), Icons.Outlined.DateRange))
        if (pomodoro) add(Destination(NavRoute.Pomodoro, stringResource(Res.string.rd_focus), Icons.Outlined.Timer))
        if (journal) add(Destination(NavRoute.Journal, stringResource(Res.string.nav_journal), Icons.Outlined.Book))
    }
    fun selected(route: NavRoute) = when(route) {
        NavRoute.Home -> current?.hasRoute<NavRoute.Home>()
        NavRoute.Todo -> current?.hasRoute<NavRoute.Todo>()
        NavRoute.Habit -> current?.hasRoute<NavRoute.Habit>()
        NavRoute.Pomodoro -> current?.hasRoute<NavRoute.Pomodoro>()
        NavRoute.Journal -> current?.hasRoute<NavRoute.Journal>()
        else -> false
    } == true
    fun navigate(route: NavRoute) {
        if (!selected(route)) navController.navigateAuraTab(route)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rail = maxWidth >= 600.dp
        val showNav = !secondary && destinations.size > 1
        CompositionLocalProvider(LocalAuraGutter provides if (maxWidth < 360.dp) 16.dp else 24.dp,
            LocalAuraAnnounce provides { message -> scope.launch { snackbar.showSnackbar(message) }; Unit }) {
            Scaffold(containerColor = AppTheme.colors.background, contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (showNav && !rail) NavigationBar(
                        containerColor = AppTheme.colors.surface, tonalElevation = AuraElevation.flat
                    ) {
                        destinations.forEach { item ->
                            NavigationBarItem(selected(item.route), { navigate(item.route) },
                                icon = { Icon(item.icon, null) },
                                label = { Text(item.title, style = AppTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(selectedTextColor = AppTheme.colors.primary,
                                    selectedIconColor = AppTheme.colors.onPrimaryContainer, indicatorColor = AppTheme.colors.primaryContainer,
                                    unselectedTextColor = AppTheme.colors.textSecondary, unselectedIconColor = AppTheme.colors.textSecondary))
                        }
                    }
                }
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    if (showNav && rail) NavigationRail(
                        modifier = Modifier.width(80.dp).fillMaxHeight(),
                        windowInsets = WindowInsets(0, 0, 0, 0), containerColor = AppTheme.colors.surface
                    ) {
                        AuraBrandMark(Modifier.padding(vertical = 24.dp))
                        destinations.forEach { item ->
                            NavigationRailItem(selected(item.route), { navigate(item.route) },
                                icon = { Icon(item.icon, null) }, label = { Text(item.title) },
                                colors = NavigationRailItemDefaults.colors(selectedTextColor = AppTheme.colors.primary,
                                    selectedIconColor = AppTheme.colors.onPrimaryContainer, indicatorColor = AppTheme.colors.primaryContainer,
                                    unselectedTextColor = AppTheme.colors.textSecondary, unselectedIconColor = AppTheme.colors.textSecondary))
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = if (secondary) AuraSpacing.readingWidth else AuraSpacing.contentWidth).fillMaxSize()) { content() }
                    }
                }
            }
        }
    }
}
