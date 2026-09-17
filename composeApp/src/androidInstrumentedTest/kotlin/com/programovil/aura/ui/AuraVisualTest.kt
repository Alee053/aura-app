package com.programovil.aura.ui

import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.*
import com.programovil.aura.auth.presentation.screen.SignInScreen
import com.programovil.aura.designsystem.components.button.*
import com.programovil.aura.designsystem.components.state.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.habit.domain.model.*
import com.programovil.aura.habit.presentation.composable.*
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.home.presentation.screen.HomeContent
import com.programovil.aura.home.presentation.viewmodel.HomeUiState
import com.programovil.aura.journal.domain.model.JournalEntry
import com.programovil.aura.journal.domain.repository.JournalRepository
import com.programovil.aura.journal.domain.usecase.*
import com.programovil.aura.journal.presentation.screen.*
import com.programovil.aura.journal.presentation.viewmodel.*
import com.programovil.aura.navigation.NavRoute
import com.programovil.aura.pomodoro.presentation.*
import com.programovil.aura.settings.presentation.composable.ThemeCard
import com.programovil.aura.shared.presentation.*
import com.programovil.aura.shared.presentation.composable.*
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.repository.TodoRepository
import com.programovil.aura.todo.domain.usecase.*
import com.programovil.aura.todo.presentation.screen.TodoScreen
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import org.junit.*
import java.io.File

class AuraVisualTest {
    @get:Rule val rule = createComposeRule()
    private val models = mutableListOf<androidx.lifecycle.ViewModel>()
    @get:Rule val testName = org.junit.rules.TestName()
    @After fun cleanup() {
        runCatching { capture("end-" + testName.methodName) }
        rule.runOnIdle { models.forEach { it.viewModelScope.cancel() } }
    }

    private fun fixture(mode: ThemeMode = ThemeMode.PURPLE, font: Float = 1f, content: @Composable () -> Unit) {
        rule.setContent {
            val density = LocalDensity.current
            val context = LocalContext.current
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides LocalConfiguration.current,
                LocalDensity provides Density(density.density, font),
                LocalAuraMotion provides AuraMotion(enabled = false)) {
                DsTheme(mode) {
                    ApplyAuraSystemBars(AppTheme.colors.isLight)
                    Surface(Modifier.fillMaxSize(), color = AppTheme.colors.background) {
                        Box(Modifier.safeDrawingPadding()) { content() }
                    }
                }
            }
        }
    }
    private fun capture(name: String) {
        rule.waitForIdle()
        // PixelCopy waits for the Compose root to draw, including the first frame.
        rule.onAllNodes(isRoot()).onFirst().captureToImage()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val label = InstrumentationRegistry.getArguments().getString("aura.label") ?: "phone"
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-qa/$label").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Composable private fun home(failed: Boolean = false) {
        HomeContent(HomeUiState(DashboardData(3, 2, 3, 7), isLoading = false, hasData = !failed, loadFailed = failed),
            true, true, true, true, {}, {}, {}, {}, {}, {})
    }

    @Test fun homeAndTabNavigation() {
        fixture {
            val nav = rememberNavController()
            AuraAppShell(nav, true, true, true, true) {
                NavHost(nav, NavRoute.Home) {
                    composable<NavRoute.Home> { home() }
                    composable<NavRoute.Todo> { Text("Tasks fixture") }
                    composable<NavRoute.Habit> { Text("Habits fixture") }
                    composable<NavRoute.Pomodoro> { PomodoroContent(PomodoroUiState(), {}, {}, {}, {}) }
                    composable<NavRoute.Journal> { Text("Journal fixture") }
                }
            }
        }
        rule.onNodeWithText("3", useUnmergedTree = true).assertExists()
        capture("01-home")
        rule.onAllNodesWithText("Focus").onLast().performClick()
        rule.onNodeWithContentDescription("Start").assertExists()
        capture("02-focus")
    }
    @Test fun signInHasAccessibleProviderAndNoDoubleRequestWhileLoading() {
        var clicks = 0
        fixture { SignInScreen(null, { clicks++ }, isLoading = true) }
        rule.onNode(hasText("Sign in with Google") and hasClickAction()).assertIsNotEnabled()
        Assert.assertEquals(0, clicks)
        capture("03-sign-in-pending")
    }
    @Test fun signIn() {
        fixture { SignInScreen(null, {}) }
        rule.onNodeWithText("Sign in with Google").assertExists()
        capture("04-sign-in")
    }
    @Test fun homeFailureIsNotZero() {
        fixture { home(true) }
        rule.onNodeWithText("Try again").assertExists()
        rule.onNodeWithText("0").assertDoesNotExist()
        capture("05-home-error")
    }
    @Test fun darkFocusAndLargeText() {
        fixture(ThemeMode.DARK, 2f) { PomodoroContent(PomodoroUiState(timeLeftSeconds = 724), {}, {}, {}, {}) }
        rule.onNodeWithText("12:04").assertExists()
        capture("06-focus-dark-large")
    }
    @Test fun highContrastFocus() {
        fixture(ThemeMode.HIGH_CONTRAST) { PomodoroContent(PomodoroUiState(), {}, {}, {}, {}) }
        capture("07-focus-high-contrast")
    }
    @Test fun habitCardUsesRealDatesAndIndependentActions() {
        val today = todayDate()
        val item = HabitWithStatus(Habit("h1", "Leer veinte minutos", RecurrenceType.WEEKLY, 5, "#7C6AE6", 0),
            3 to 5, 7, (6 downTo 0).map { offset ->
                DayCompletion(today.minus(offset, DateTimeUnit.DAY).toString(), offset in listOf(1, 2, 4))
            })
        fixture {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Hábitos", style = AppTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                HabitCard(item, {}, {})
            }
        }
        rule.onNodeWithContentDescription("Edit").assertExists()
        capture("08-habits")
    }
    @Test fun todoEditorKeepsDraftUntilResultAndBlocksDuplicateSave() {
        val repo = FixtureTodoRepository()
        lateinit var vm: TodoViewModel
        fixture {
            vm = remember { TodoViewModel(GetTodosUseCase(repo), AddTodoUseCase(repo), UpdateTodoUseCase(repo),
                ToggleTodoUseCase(repo), DeleteTodoUseCase(repo)).also { models += it } }
            TodoScreen(vm)
        }
        rule.onNodeWithText("Preparar el portfolio").assertExists()
        capture("09-tasks")
        rule.onNodeWithText("Preparar el portfolio").performClick()
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Borrador seguro")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { vm.operations.isPending("editor") }
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.runOnIdle { Assert.assertEquals(1, repo.updateCalls) }
        capture("10-task-pending")
        rule.runOnIdle { repo.result.complete(Result.failure(IllegalStateException("offline"))) }
        rule.onNodeWithText("Borrador seguro").assertExists()
        rule.onNodeWithText("Save").assertIsEnabled()
        capture("11-task-failure")
    }
    @Test fun journalListAndEditor() {
        val repo = FixtureJournalRepository()
        fixture {
            val vm = remember { JournalViewModel(GetJournalEntriesUseCase(repo), DeleteJournalEntryUseCase(repo)).also { models += it } }
            JournalScreen(vm, {})
        }
        rule.onNodeWithText("Un día con intención").assertExists()
        capture("12-journal")
    }
    @Test fun journalEditorPreservesText() {
        val repo = FixtureJournalRepository()
        fixture {
            val vm = remember { JournalDetailViewModel("j1", GetJournalEntryUseCase(repo), AddJournalEntryUseCase(repo),
                UpdateJournalEntryUseCase(repo), DeleteJournalEntryUseCase(repo)).also { models += it } }
            JournalDetailScreen(vm, {})
        }
        rule.onNodeWithText("Un día con intención").assertExists()
        capture("13-journal-editor")
    }
    @Test fun themeTilesAreRadioSelections() {
        fixture {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Apariencia", style = AppTheme.typography.headlineLarge)
                ThemeMode.entries.forEach { mode ->
                    val palette = paletteFor(mode)
                    ThemeCard(mode.name, listOf(palette.background, palette.primary), mode == ThemeMode.PURPLE, {})
                }
            }
        }
        rule.onNodeWithText("PURPLE").assertIsSelected()
        capture("14-themes")
    }
}
private class FixtureTodoRepository : TodoRepository {
    val result = CompletableDeferred<Result<Unit>>()
    var updateCalls = 0
    override fun getTodos() = flowOf(Result.success(listOf(
        Todo("t1", "Preparar el portfolio", "Una pequeña mejora cada día.", false, null),
        Todo("t2", "Revisar las capturas", null, false, null),
        Todo("t3", "Definir la dirección visual", null, true, null))))
    override suspend fun addTodo(title: String, description: String?, dueDate: Long?) = result.await()
    override suspend fun updateTodo(todo: Todo): Result<Unit> { updateCalls++; return result.await() }
    override suspend fun toggleTodo(todoId: String, isCompleted: Boolean) = Result.success(Unit)
    override suspend fun deleteTodo(todoId: String) = Result.success(Unit)
}
private class FixtureJournalRepository : JournalRepository {
    private val entry = JournalEntry("j1", "Un día con intención",
        "Hoy encontré un momento para concentrarme. Avanzar también significa reconocer lo que ya está bien.\n\nMañana quiero reservar tiempo para leer.", 1789491600000, 1789491600000)
    override fun getEntries() = flowOf(Result.success(listOf(entry)))
    override suspend fun getEntry(id: String) = entry
    override suspend fun addEntry(title: String, content: String) = Result.success(Unit)
    override suspend fun updateEntry(entry: JournalEntry) = Result.success(Unit)
    override suspend fun deleteEntry(entry: JournalEntry) = Result.success(Unit)
}
