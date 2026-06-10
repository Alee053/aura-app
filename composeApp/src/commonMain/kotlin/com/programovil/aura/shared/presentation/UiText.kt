package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.error_journal_not_found
import aura_app.composeapp.generated.resources.error_journal_load
import aura_app.composeapp.generated.resources.error_journal_save
import aura_app.composeapp.generated.resources.error_journal_delete
import aura_app.composeapp.generated.resources.error_habit_load
import aura_app.composeapp.generated.resources.error_habit_add
import aura_app.composeapp.generated.resources.error_habit_update
import aura_app.composeapp.generated.resources.error_habit_delete
import aura_app.composeapp.generated.resources.error_todo_load
import aura_app.composeapp.generated.resources.error_todo_add
import aura_app.composeapp.generated.resources.error_todo_update
import aura_app.composeapp.generated.resources.error_todo_delete
import aura_app.composeapp.generated.resources.error_unknown

sealed interface UiText {
    data class Resource(val id: StringResource) : UiText
    data class Dynamic(val value: String) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Resource -> stringResource(id)
        is Dynamic -> value
    }
}

object ErrorKey {
    val Unknown: UiText = UiText.Resource(Res.string.error_unknown)
    val TodoLoad: UiText = UiText.Resource(Res.string.error_todo_load)
    val TodoAdd: UiText = UiText.Resource(Res.string.error_todo_add)
    val TodoUpdate: UiText = UiText.Resource(Res.string.error_todo_update)
    val TodoDelete: UiText = UiText.Resource(Res.string.error_todo_delete)
    val HabitLoad: UiText = UiText.Resource(Res.string.error_habit_load)
    val HabitAdd: UiText = UiText.Resource(Res.string.error_habit_add)
    val HabitUpdate: UiText = UiText.Resource(Res.string.error_habit_update)
    val HabitDelete: UiText = UiText.Resource(Res.string.error_habit_delete)
    val JournalLoad: UiText = UiText.Resource(Res.string.error_journal_load)
    val JournalSave: UiText = UiText.Resource(Res.string.error_journal_save)
    val JournalDelete: UiText = UiText.Resource(Res.string.error_journal_delete)
    val JournalNotFound: UiText = UiText.Resource(Res.string.error_journal_not_found)
}
