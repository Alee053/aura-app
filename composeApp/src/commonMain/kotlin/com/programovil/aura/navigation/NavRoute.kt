package com.programovil.aura.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Home : NavRoute()

    @Serializable
    data object Tasks : NavRoute()

    @Serializable
    data object Focus : NavRoute()

    @Serializable
    data object Progress : NavRoute()

    @Serializable
    data object Settings : NavRoute()

    @Serializable
    data object Todo : NavRoute()

    @Serializable
    data class TodoDetail(val todoId: String) : NavRoute()

    @Serializable
    data object Habit : NavRoute()
}
