package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable

@Composable expect fun AuraBackHandler(enabled: Boolean = true, onBack: () -> Unit)
@Composable expect fun systemAnimationsEnabled(): Boolean
@Composable expect fun uses24HourClock(): Boolean

@Composable
expect fun ApplyAuraSystemBars(light: Boolean)
