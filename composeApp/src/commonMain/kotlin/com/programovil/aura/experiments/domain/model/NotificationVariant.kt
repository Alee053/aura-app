package com.programovil.aura.experiments.domain.model

data class NotificationVariant(
    val timesPerDay: Int,
    val tone: Tone,
    val useDueDateChannel: Boolean
)
