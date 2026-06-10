package com.programovil.aura.shared

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MotivationPhraseManagerTest {

    @Test
    fun `parses valid JSON into locale map`() = runTest(UnconfinedTestDispatcher()) {
        val json = """{"en":"Stay focused","es":"Mantén el enfoque","fr":"Reste concentré"}"""
        val manager = MotivationPhraseManager(
            FakeRemoteConfigService(stringValues = mapOf(StringRemoteConfigFlag.MOTIVATION_PHRASE.key to json))
        )
        manager.initialize()
        kotlinx.coroutines.delay(100)

        manager.phrase.test {
            val phrases = awaitItem()
            assertEquals("Stay focused", phrases["en"])
            assertEquals("Mantén el enfoque", phrases["es"])
            assertEquals("Reste concentré", phrases["fr"])
            cancelAndIgnoreRemainingEvents()
        }
        manager.stop()
    }

    @Test
    fun `malformed JSON falls back to empty map`() = runTest(UnconfinedTestDispatcher()) {
        val manager = MotivationPhraseManager(
            FakeRemoteConfigService(stringValues = mapOf(StringRemoteConfigFlag.MOTIVATION_PHRASE.key to "not json"))
        )
        manager.initialize()
        kotlinx.coroutines.delay(100)

        manager.phrase.test {
            val phrases = awaitItem()
            assertEquals(emptyMap(), phrases)
            cancelAndIgnoreRemainingEvents()
        }
        manager.stop()
    }
}
