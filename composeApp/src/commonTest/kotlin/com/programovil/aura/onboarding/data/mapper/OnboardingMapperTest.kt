package com.programovil.aura.onboarding.data.mapper

import com.programovil.aura.onboarding.data.dto.OnboardingSlideDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingMapperTest {

    private val sampleDto = OnboardingSlideDto(
        id = 1,
        title = mapOf("es" to "Título ES", "en" to "Title EN", "fr" to "Titre FR"),
        description = mapOf("es" to "Desc ES", "en" to "Desc EN", "fr" to "Desc FR"),
        imageUrl = mapOf("es" to "https://es.png", "en" to "", "fr" to "https://fr.png")
    )

    @Test
    fun `toDomain resolves Spanish locale`() {
        val result = sampleDto.toDomain("es")
        assertEquals("Título ES", result.title)
        assertEquals("Desc ES", result.description)
        assertEquals("https://es.png", result.imageUrl)
    }

    @Test
    fun `toDomain resolves English locale`() {
        val result = sampleDto.toDomain("en")
        assertEquals("Title EN", result.title)
        assertEquals("Desc EN", result.description)
        assertEquals(null, result.imageUrl)
    }

    @Test
    fun `toDomain resolves French locale`() {
        val result = sampleDto.toDomain("fr")
        assertEquals("Titre FR", result.title)
        assertEquals("Desc FR", result.description)
        assertEquals("https://fr.png", result.imageUrl)
    }

    @Test
    fun `toDomain falls back to English for unsupported locale`() {
        val result = sampleDto.toDomain("de")
        assertEquals("Title EN", result.title)
        assertEquals("Desc EN", result.description)
    }

    @Test
    fun `toDomain falls back to English when locale key missing`() {
        val dto = sampleDto.copy(title = mapOf("en" to "Only EN"))
        val result = dto.toDomain("ja")
        assertEquals("Only EN", result.title)
    }

    @Test
    fun `toDomain returns empty string when no locale and no English fallback`() {
        val dto = sampleDto.copy(title = mapOf("de" to "German only"))
        val result = dto.toDomain("ja")
        assertEquals("", result.title)
    }

    @Test
    fun `toDomain returns null imageUrl for blank string`() {
        val result = sampleDto.toDomain("en")
        assertNull(result.imageUrl)
    }

    @Test
    fun `toDomain preserves id`() {
        val result = sampleDto.toDomain("en")
        assertEquals(1, result.id)
    }
}