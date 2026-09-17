package com.programovil.aura.onboarding.presentation.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import aura_app.composeapp.generated.resources.*
import com.programovil.aura.designsystem.components.button.AuraButton
import com.programovil.aura.designsystem.components.state.AuraSkeleton
import com.programovil.aura.designsystem.theme.*
import com.programovil.aura.onboarding.presentation.OnboardingViewModel
import com.programovil.aura.shared.presentation.composable.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(onSkip: () -> Unit, onStart: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val motion = LocalAuraMotion.current
    Box(Modifier.fillMaxSize().background(AppTheme.colors.background).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        when(val current = state) {
            OnboardingViewModel.OnboardingState.Loading -> AuraSkeleton(Modifier.padding(AuraSpacing.lg), label = stringResource(Res.string.rd_loading))
            is OnboardingViewModel.OnboardingState.Error -> LaunchedEffect(current) { onSkip() }
            is OnboardingViewModel.OnboardingState.Completed -> LaunchedEffect(current) { if (current.skipped) onSkip() else onStart() }
            is OnboardingViewModel.OnboardingState.Loaded -> {
                val pager = rememberPagerState { current.slides.size }
                LaunchedEffect(current.currentIndex) {
                    if (motion.enabled) pager.animateScrollToPage(current.currentIndex, animationSpec = tween(280))
                    else pager.scrollToPage(current.currentIndex)
                }
                Column(Modifier.widthIn(max = AuraSpacing.editorWidth).fillMaxSize().padding(AuraSpacing.lg)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AuraBrandMark(Modifier.size(32.dp))
                        Spacer(Modifier.weight(1f))
                        TextButton(viewModel::skip) { Text(stringResource(Res.string.onboarding_skip)) }
                    }
                    HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), userScrollEnabled = false) { page ->
                        val slide = current.slides[page]
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = AuraSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(AuraSpacing.lg)) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AuraScene(slide.id, Modifier.width(220.dp)) }
                            Text(stringResource(Res.string.rd_onboarding_page, page + 1, current.slides.size),
                                color = AppTheme.colors.primary, style = AppTheme.typography.labelLarge)
                            Text(slide.title, style = AppTheme.typography.headlineLarge)
                            Text(slide.description, style = AppTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary)
                        }
                    }
                    Row(Modifier.padding(vertical = AuraSpacing.md), horizontalArrangement = Arrangement.spacedBy(AuraSpacing.xs)) {
                        repeat(current.slides.size) { index ->
                            val width by animateDpAsState(if (index == current.currentIndex) 28.dp else 8.dp,
                                tween(motion.state), label = "onboarding indicator")
                            Box(Modifier.width(width).height(6.dp).clip(CircleShape)
                                .background(if (index == current.currentIndex) AppTheme.colors.primary else AppTheme.colors.outline))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                        if (current.currentIndex > 0) TextButton(viewModel::previous) { Text(stringResource(Res.string.onboarding_previous)) }
                        AuraButton(stringResource(if (current.isLastSlide) Res.string.onboarding_start else Res.string.onboarding_next),
                            if (current.isLastSlide) viewModel::start else viewModel::next, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
