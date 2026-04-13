package com.programovil.aura.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    data object Todo : NavRoute()
}