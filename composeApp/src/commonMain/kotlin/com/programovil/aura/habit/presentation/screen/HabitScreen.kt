package com.programovil.aura.habit.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.header.AuraScreenHeader
import com.programovil.aura.designsystem.components.state.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCase
import com.programovil.aura.habit.presentation.composable.*
import com.programovil.aura.habit.presentation.viewmodel.*
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HabitScreen(featureFlags: Map<FeatureFlag, Boolean> = emptyMap(), onFeatureDisabled: () -> Unit = {},
    viewModel: HabitViewModel = koinViewModel(),
    getHabitsAccessibilityUseCase: GetHabitsAccessibilityUseCase = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    val operations by viewModel.operations.states.collectAsState()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingHabit by rememberSaveable(stateSaver = HabitEditorSaver) { mutableStateOf<Habit?>(null) }
    val selected = editingHabit ?: state.habits.find { it.habit.id == editingId }?.habit
    val editor = operations["editor"]
    val openNew = { viewModel.operations.clearCompleted(); editingId = null; editingHabit = null; showEditor = true }
    ObserveToggleFeedback(operations, viewModel.operations)
    LaunchedEffect(editor?.requestId, editor?.status) {
        if (editor?.status == OperationStatus.Succeeded) {
            showEditor = false; editingHabit = null; editingId = null
            viewModel.operations.consume("editor", editor.requestId)
        }
    }
    Scaffold(containerColor = AppTheme.colors.background, contentWindowInsets = WindowInsets(0,0,0,0),
        floatingActionButton = {
            if (state.habits.isNotEmpty()) ExtendedFloatingActionButton(onClick = openNew,
                icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(stringResource(Res.string.rd_new_habit)) },
                containerColor = AppTheme.colors.primary, contentColor = AppTheme.colors.onPrimary)
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = LocalAuraGutter.current),
            verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm),
            contentPadding = PaddingValues(bottom = AuraSpacing.section + AuraSpacing.xl)) {
            item { AuraScreenHeader(stringResource(Res.string.habits_title), auraDate(todayDate())) }
            state.loadError?.let { error -> item {
                AuraInlineNotice(error.asString(), actionLabel = stringResource(Res.string.rd_retry), onAction = viewModel::retryLoad)
            } }
            operations.values.firstOrNull { it.kind == "toggle" && it.status == OperationStatus.Failed }?.let { operation ->
                item { AuraInlineNotice(operation.error!!.asString(), actionLabel = stringResource(Res.string.rd_close),
                    onAction = { viewModel.operations.consume(operation.target, operation.requestId) }) }
            }
            operations.values.firstOrNull { it.pending && it.longRunning }?.let { op ->
                item { OperationNotice(op) }
            }
            when {
                state.isLoading -> item { AuraSkeleton(rows = 2, label = stringResource(Res.string.rd_loading)) }
                state.habits.isEmpty() && state.loadError == null -> item {
                    AuraEmptyState(stringResource(Res.string.rd_empty_habits), stringResource(Res.string.rd_empty_habits_body),
                        stringResource(Res.string.rd_new_habit), openNew,
                        illustration = { AuraScene(2, Modifier.size(AuraSpacing.control * 2)) })
                }
                else -> {
                    item {
                        Text("${stringResource(Res.string.rd_marked_today)} · ${state.habits.count { it.last7Days.lastOrNull()?.isCompleted == true }} / ${state.habits.size}",
                            Modifier.padding(bottom = AuraSpacing.md), style = AppTheme.typography.labelLarge, color = AppTheme.colors.primary)
                    }
                    items(state.habits, key = { it.habit.id }) { item ->
                        HabitCard(item, { date -> viewModel.onEvent(HabitEvent.ToggleCompletion(item.habit.id, date)) },
                            { viewModel.operations.clearCompleted(); editingId = item.habit.id; editingHabit = item.habit; showEditor = true },
                            Modifier.animateItem(), pendingDates = operations.values.filter { it.pending && it.target.startsWith("toggle:${item.habit.id}:") }
                                .map { it.target.substringAfterLast(":") }.toSet())
                    }
                    item { Text(stringResource(Res.string.rd_streak_help), Modifier.padding(top = AuraSpacing.md),
                        style = AppTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary) }
                }
            }
        }
    }
    if (showEditor) HabitDialog(selected, { showEditor = false; editingId = null; editingHabit = null },
        { viewModel.onEvent(HabitEvent.UpdateHabit(it)) },
        selected?.let { { viewModel.onEvent(HabitEvent.DeleteHabit(it.id)) } }, editor)
}
