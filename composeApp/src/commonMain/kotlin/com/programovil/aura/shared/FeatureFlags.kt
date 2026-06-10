package com.programovil.aura.shared

enum class FeatureFlag(
    val key: String,
    val defaultValue: Boolean
) {
    HABITS_ENABLED("habits_enabled", true),
    TODOS_ENABLED("todos_enabled", true),
    JOURNAL_ENABLED("journal_enabled", true),
    POMODORO_ENABLED("pomodoro_enabled", true),
    IS_PREMIUM("is_premium", false);
}
