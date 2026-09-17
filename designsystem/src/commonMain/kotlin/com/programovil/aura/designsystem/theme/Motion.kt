package com.programovil.aura.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

data class AuraMotion(val enabled: Boolean = true) {
    fun duration(millis: Int) = if (enabled) millis else 0
    val interaction get() = duration(140)
    val state get() = duration(180)
    val navigation get() = duration(240)
}
val LocalAuraMotion = staticCompositionLocalOf { AuraMotion() }
