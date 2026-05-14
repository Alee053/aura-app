package com.programovil.aura.shared

enum class FeatureFlag(
    val key: String,
    val defaultValue: Boolean
) {
    HABITS_ENABLED("habits_enabled", true),
    TODOS_ENABLED("todos_enabled", true),
}
