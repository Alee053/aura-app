package com.programovil.aura.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Home : NavRoute()

    @Serializable
    data object Todo : NavRoute()

    @Serializable
    data object Habit : NavRoute()

    @Serializable
    data object Settings : NavRoute()
}
