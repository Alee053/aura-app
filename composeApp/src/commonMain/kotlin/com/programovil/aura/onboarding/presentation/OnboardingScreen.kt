package com.programovil.aura.onboarding.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import aura_app.composeapp.generated.resources.onboarding_next
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.background),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.background,
                        AppTheme.colors.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button - visible and clickable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_skip),
                    color = AppTheme.colors.textPrimary.copy(alpha = 0.7f),
                    style = AppTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSkip() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingSlideContent(slide = slides[page], pageIndex = page)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page indicators - larger and more visible
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(slides.size) { index ->
                    val isActive = index == currentPage
                    val dotColor = if (isActive) AppTheme.colors.primary else AppTheme.colors.textPrimary.copy(alpha = 0.25f)
                    val dotSize = if (isActive) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = dotSize, height = 8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            scope.launch { onPrevious() }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surface)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Next or Start button
                if (currentPage < slides.size - 1) {
                    Button(
                        onClick = {
                            scope.launch { onNext() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.onboarding_next),
                            style = AppTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = { onStart() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            contentColor = AppTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.onboarding_start),
                            style = AppTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide, pageIndex: Int) {
    val locale = java.util.Locale.getDefault().language
    val languageCode = when {
        locale.startsWith("es") -> "es"
        locale.startsWith("fr") -> "fr"
        else -> "en"
    }

    val icon = when (pageIndex) {
        0 -> Icons.Default.Checklist
        1 -> Icons.Default.DateRange
        2 -> Icons.Default.Insights
        3 -> Icons.Default.RocketLaunch
        else -> Icons.Default.RocketLaunch
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with gradient background
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.primary.copy(alpha = 0.3f),
                            AppTheme.colors.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = slide.title[languageCode] ?: slide.title["en"] ?: "",
            style = AppTheme.typography.headlineLarge,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = slide.description[languageCode] ?: slide.description["en"] ?: "",
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.textPrimary.copy(alpha = 0.75f),
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
