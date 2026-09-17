package com.programovil.aura

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programovil.aura.shared.presentation.AuthenticatedSessionViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.auth.presentation.AuthViewModel
import com.programovil.aura.auth.presentation.screen.SignInScreen
import com.programovil.aura.auth.presentation.screen.authErrorMessage
import com.programovil.aura.designsystem.components.overlay.*
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.habit.domain.usecase.GetHabitsAccessibilityUseCase
import com.programovil.aura.navigation.AppNavHost
import com.programovil.aura.onboarding.data.OnboardingPreferences
import com.programovil.aura.onboarding.presentation.screen.OnboardingScreen
import com.programovil.aura.pomodoro.presentation.*
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import com.programovil.aura.shared.*
import com.programovil.aura.shared.presentation.systemAnimationsEnabled
import com.programovil.aura.shared.presentation.ApplyAuraSystemBars
import com.programovil.aura.shared.presentation.composable.*
import com.programovil.aura.todo.presentation.viewmodel.TodoViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(onSignInClick: () -> Unit = {}, credentialRequestInFlight: Boolean = false) {
    val settings: SettingsViewModel = koinViewModel()
    val settingsState by settings.uiState.collectAsState()
    val animationEnabled = systemAnimationsEnabled()
    DsTheme(settingsState.themeMode) {
        ApplyAuraSystemBars(AppTheme.colors.isLight)
        CompositionLocalProvider(LocalAuraMotion provides AuraMotion(animationEnabled)) {
            val session = viewModel { AuthenticatedSessionViewModel() }
            val auth: AuthViewModel = koinViewModel()
            val authState by auth.authState.collectAsState()
            val preferences: OnboardingPreferences = koinInject()
            val onboardingCompleted by preferences.isOnboardingCompleted.collectAsState(initial = null)
            var sessionGeneration by rememberSaveable { mutableIntStateOf(0) }
            var dismissed by remember { mutableStateOf(false) }
            var attemptedLogin by remember { mutableStateOf(false) }
            LaunchedEffect(authState) {
                if (authState is AuthViewModel.AuthState.SignedOut || authState is AuthViewModel.AuthState.Error) {
                    session.clearSession()
                    sessionGeneration++
                    dismissed = false
                }
            }
            when {
                authState is AuthViewModel.AuthState.SignedIn && onboardingCompleted == null -> SessionLoading()
                authState is AuthViewModel.AuthState.SignedIn && onboardingCompleted == false && !dismissed ->
                    CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                        OnboardingScreen(onSkip = { dismissed = true }, onStart = {})
                    }
                authState is AuthViewModel.AuthState.SignedIn -> {
                    // All authenticated state belongs to one session, never to a subsequent account.
                    CompositionLocalProvider(LocalViewModelStoreOwner provides session) {
                        key(sessionGeneration) {
                            AuthenticatedApp(settingsState.themeMode, settings::setThemeMode, auth::signOut)
                        }
                    }
                }
                authState is AuthViewModel.AuthState.Loading && !attemptedLogin -> SessionLoading()
                else -> SignInScreen(
                    errorMessage = (authState as? AuthViewModel.AuthState.Error)?.let { authErrorMessage(it.error) },
                    onSignInClick = { attemptedLogin = true; onSignInClick() },
                    isLoading = credentialRequestInFlight || authState is AuthViewModel.AuthState.Loading
                )
            }
        }
    }
}

@Composable
private fun SessionLoading() {
    Column(Modifier.fillMaxSize().background(AppTheme.colors.background).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AuraBrandMark(Modifier.size(AuraSpacing.control))
        Spacer(Modifier.height(AuraSpacing.lg))
        Text(stringResource(Res.string.rd_preparing), color = AppTheme.colors.textSecondary)
        LinearProgressIndicator(Modifier.width(AuraSpacing.editorWidth / 3).padding(top = AuraSpacing.lg))
    }
}

@Composable
fun AuthenticatedApp(currentThemeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val todo: TodoViewModel = koinViewModel()
    val pomodoro: PomodoroViewModel = koinViewModel()
    val timer by pomodoro.uiState.collectAsState()
    val flagsManager: FeatureFlagManager = koinInject()
    val flags by flagsManager.flags.collectAsState()
    val motivation: MotivationPhraseManager = koinInject()
    val access: GetHabitsAccessibilityUseCase = koinInject()
    val habits by access().collectAsState(initial = false)
    val overlays = remember { AuraOverlayRegistry() }
    LaunchedEffect(Unit) { flagsManager.initialize(); motivation.initialize() }
    LaunchedEffect(pomodoro) { pomodoro.syncTimerState() }
    LaunchedEffect(pomodoro) { AppLifecycleEvents.foregroundEvents.collect { pomodoro.syncTimerState() } }
    LaunchedEffect(pomodoro) { PomodoroLaunchEvents.events.collect { pomodoro.syncTimerState() } }
    val showPomodoro = flags[FeatureFlag.POMODORO_ENABLED] != false
    CompositionLocalProvider(LocalAuraOverlays provides overlays) {
        AuraAppShell(nav, flags[FeatureFlag.TODOS_ENABLED] != false, habits,
            showPomodoro, flags[FeatureFlag.JOURNAL_ENABLED] != false) {
            AppNavHost(nav, todo, pomodoro, currentThemeMode, onThemeChange, onSignOut, flags, habits)
        }
        if (showPomodoro && timer.showCompletionMessage && overlays.count == 0) {
            PomodoroCompletionOverlay(timer.completedMode, pomodoroModeLabel(timer.mode), pomodoro::dismissCompletionMessage)
        }
    }
}
