package com.programovil.aura.onboarding.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.components.button.PrimaryButton
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.onboarding.presentation.OnboardingViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.onboarding_skip
import aura_app.composeapp.generated.resources.onboarding_next
import aura_app.composeapp.generated.resources.onboarding_previous
import aura_app.composeapp.generated.resources.onboarding_start

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onStart: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is OnboardingViewModel.OnboardingState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        }
        is OnboardingViewModel.OnboardingState.Loaded -> {
            OnboardingContent(
                slides = currentState.slides,
                currentIndex = currentState.currentIndex,
                isLastSlide = currentState.isLastSlide,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onSkip = viewModel::skip,
                onStart = viewModel::start
            )
        }
        is OnboardingViewModel.OnboardingState.Completed -> {
            LaunchedEffect(currentState) {
                if (currentState.skipped) {
                    onSkip()
                } else {
                    onStart()
                }
            }
        }
        is OnboardingViewModel.OnboardingState.Error -> {
            LaunchedEffect(currentState) { onSkip() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingContent(
    slides: List<OnboardingSlide>,
    currentIndex: Int,
    isLastSlide: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastSlide) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(Res.string.onboarding_skip),
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false
            ) { page ->
                SlideContent(slide = slides[page])
            }

            Spacer(modifier = Modifier.weight(1f))

            PageIndicator(
                totalPages = slides.size,
                currentPage = currentIndex
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentIndex > 0) {
                    PrimaryButton(
                        text = stringResource(Res.string.onboarding_previous),
                        onClick = onPrevious
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (isLastSlide) {
                    PrimaryButton(
                        text = stringResource(Res.string.onboarding_start),
                        onClick = onStart
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.onboarding_next),
                        onClick = onNext
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (slide.id) {
            1 -> Icons.Default.Checklist
            2 -> Icons.Default.SelfImprovement
            3 -> Icons.Default.Edit
            4 -> Icons.Default.Dashboard
            else -> Icons.Default.Checklist
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = AppTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = slide.title,
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = slide.description,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicator(
    totalPages: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) AppTheme.colors.primary
                        else AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
