package com.programovil.aura.shared

enum class FeatureFlag(
    val key: String,
    val defaultValue: Boolean
) {
    HABITS_ENABLED("habits_enabled", true),
    TODOS_ENABLED("todos_enabled", true),
    JOURNAL_ENABLED("journal_enabled", true),
    POMODORO_ENABLED("pomodoro_enabled", true),
}

enum class UserPlanFlag(
    val key: String,
    val defaultValue: String
) {
    USER_PLAN("user_plan", "Free");
}
