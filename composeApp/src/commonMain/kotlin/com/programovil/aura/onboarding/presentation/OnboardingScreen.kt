package com.programovil.aura.onboarding.presentation

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programovil.aura.designsystem.theme.AppTheme
import com.programovil.aura.designsystem.theme.DsTheme
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import aura_app.composeapp.generated.resources.Res
import aura_app.composeapp.generated.resources.onboarding_skip
import aura_app.composeapp.generated.resources.onboarding_start
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val navigateToHome by viewModel.navigateToHome.collectAsState()

    LaunchedEffect(navigateToHome) {
        if (navigateToHome) {
            onNavigateToHome()
            viewModel.onNavigated()
        }
    }

    when (uiState) {
        is OnboardingUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        }

        is OnboardingUiState.Content -> {
            val slides = (uiState as OnboardingUiState.Content).slides
            OnboardingContent(
                slides = slides,
                currentPage = currentPage,
                onNext = viewModel::nextPage,
                onPrevious = viewModel::previousPage,
                onSkip = viewModel::skip,
                onStart = viewModel::start
            )
        }

        is OnboardingUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (uiState as OnboardingUiState.Error).message,
                    color = AppTheme.colors.error,
                    style = AppTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun OnboardingContent(
    slides: List<OnboardingSlide>,
    currentPage: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentPage) {
        pagerState.animateScrollToPage(currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(Res.string.onboarding_skip),
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingSlideContent(slide = slides[page])
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            repeat(slides.size) { index ->
                val dotColor = if (index == currentPage) AppTheme.colors.primary else AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(color = dotColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            if (currentPage > 0) {
                IconButton(
                    onClick = {
                        scope.launch {
                            onPrevious()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Next or Start button
            if (currentPage < slides.size - 1) {
                IconButton(
                    onClick = {
                        scope.launch {
                            onNext()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AppTheme.colors.primary
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.onboarding_start),
                    color = AppTheme.colors.primary,
                    style = AppTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    val locale = java.util.Locale.getDefault().language
    val languageCode = when {
        locale.startsWith("es") -> "es"
        locale.startsWith("fr") -> "fr"
        else -> "en"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Placeholder image area
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = slide.title[languageCode] ?: slide.title["en"] ?: "",
            style = AppTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = slide.description[languageCode] ?: slide.description["en"] ?: "",
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun OnboardingPreview() {
    DsTheme(mode = ThemeMode.PURPLE) {
        OnboardingContent(
            slides = listOf(
                OnboardingSlide(
                    id = 1,
                    title = mapOf("es" to "¡Organiza tu día!", "en" to "Organize your day!"),
                    description = mapOf("es" to "Gestiona tareas con AURA.", "en" to "Manage tasks with AURA."),
                    image_url = mapOf("es" to "", "en" to "")
                ),
                OnboardingSlide(
                    id = 2,
                    title = mapOf("es" to "Todo listo", "en" to "All ready"),
                    description = mapOf("es" to "Crea tu cuenta ahora.", "en" to "Create your account now."),
                    image_url = mapOf("es" to "", "en" to "")
                )
            ),
            currentPage = 0,
            onNext = {},
            onPrevious = {},
            onSkip = {},
            onStart = {}
        )
    }
}
