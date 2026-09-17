package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable

// Android is the delivery target; the existing iOS host has no authenticated flows.
@Composable actual fun AuraBackHandler(enabled: Boolean, onBack: () -> Unit) {}
@Composable actual fun systemAnimationsEnabled() = false
@Composable actual fun uses24HourClock() = true

@Composable
actual fun ApplyAuraSystemBars(light: Boolean) = Unit
