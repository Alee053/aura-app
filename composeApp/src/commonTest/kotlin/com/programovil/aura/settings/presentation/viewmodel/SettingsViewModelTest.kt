package com.programovil.aura.settings.presentation.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.viewModelScope
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import com.programovil.aura.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val themeFlow = MutableStateFlow(ThemeMode.PURPLE)
    private val scheduler = FakeNotificationScheduler()
    private val themeRepository = FakeThemeRepository(themeFlow)
    private val notificationDataStore = InMemoryPreferenceDataStore()
    private val notificationPreferences = NotificationPreferences(notificationDataStore)
    private val viewModels = mutableListOf<SettingsViewModel>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects default values before any flow emits`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.PURPLE, state.themeMode)
        assertEquals(false, state.notificationsEnabled)
        assertEquals(8, state.notificationHour)
        assertEquals(0, state.notificationMinute)
    }

    @Test
    fun `theme mode emission updates the state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        themeFlow.value = ThemeMode.DARK
        runCurrent()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `enabling notifications schedules the daily summary`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.setNotificationsEnabled(true)
        runCurrent()

        assertTrue(viewModel.uiState.value.notificationsEnabled)
        assertEquals(8, scheduler.scheduledHour)
        assertEquals(0, scheduler.scheduledMinute)
    }

    @Test
    fun `disabling notifications cancels the scheduled summary`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.setNotificationsEnabled(true)
        runCurrent()
        scheduler.reset()

        viewModel.setNotificationsEnabled(false)
        runCurrent()

        assertEquals(false, viewModel.uiState.value.notificationsEnabled)
        assertEquals(1, scheduler.cancelledCount)
    }

    @Test
    fun `changing the hour reschedules when notifications are enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.setNotificationsEnabled(true)
        runCurrent()
        scheduler.reset()

        viewModel.setNotificationTime(7, 0)
        runCurrent()

        assertEquals(7, viewModel.uiState.value.notificationHour)
        assertEquals(7, scheduler.scheduledHour)
        assertEquals(0, scheduler.scheduledMinute)
    }

    @Test
    fun `changing the minute reschedules when notifications are enabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.setNotificationsEnabled(true)
        runCurrent()
        scheduler.reset()

        viewModel.setNotificationTime(8, 30)
        runCurrent()

        assertEquals(30, viewModel.uiState.value.notificationMinute)
        assertEquals(8, scheduler.scheduledHour)
        assertEquals(30, scheduler.scheduledMinute)
    }

    @Test
    fun `setThemeMode calls the repository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setThemeMode(ThemeMode.GREEN)
        runCurrent()

        assertEquals(ThemeMode.GREEN, themeRepository.lastSet)
    }

    @Test
    fun `setNotificationsEnabled writes to preferences`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setNotificationsEnabled(true)
        runCurrent()

        val prefs = notificationDataStore.data.first()
        assertEquals(true, prefs[DAILY_SUMMARY_ENABLED])
    }

    @Test
    fun `setNotificationTime writes hour and minute`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setNotificationTime(9, 45)
        runCurrent()

        val prefs = notificationDataStore.data.first()
        assertEquals(9, prefs[NOTIFICATION_HOUR])
        assertEquals(45, prefs[NOTIFICATION_MINUTE])
    }

    private fun createViewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(themeRepository, notificationPreferences, scheduler)
        viewModels += viewModel
        return viewModel
    }
}

private val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")

private class FakeThemeRepository(
    private val flow: MutableStateFlow<ThemeMode>
) : ThemeRepository {
    var lastSet: ThemeMode? = null

    override fun getThemeMode(): Flow<ThemeMode> = flow

    override suspend fun setThemeMode(mode: ThemeMode) {
        lastSet = mode
    }
}

private class FakeNotificationScheduler : NotificationScheduler {
    var scheduledHour: Int? = null
    var scheduledMinute: Int? = null
    var cancelledCount: Int = 0

    override fun scheduleDailySummary(hour: Int, minute: Int) {
        scheduledHour = hour
        scheduledMinute = minute
    }

    override fun cancelDailySummary() {
        cancelledCount += 1
    }

    override fun schedulePomodoroCompletion(delayMillis: Long) = Unit
    override fun cancelPomodoroCompletion() = Unit
    override fun showPomodoroCompletionNow() = Unit
    override fun testNotification() = Unit

    fun reset() {
        scheduledHour = null
        scheduledMinute = null
        cancelledCount = 0
    }
}

private class InMemoryPreferenceDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences
    ): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}
