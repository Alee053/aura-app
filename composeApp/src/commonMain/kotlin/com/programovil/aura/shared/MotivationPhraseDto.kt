package com.programovil.aura.shared

import kotlinx.serialization.Serializable

/**
 * JSON DTO for the `motivation_phrase` remote-config String parameter.
 * Wire format (stored as String in Firebase):
 * ```json
 * {"en":"Stay focused","es":"Mantén el enfoque","fr":"Reste concentré"}
 * ```
 */
@Serializable
data class MotivationPhraseDto(
    val phrases: Map<String, String> = emptyMap()
)
