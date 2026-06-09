package com.programovil.aura.shared

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteConfigValueManagerTest {

    @Test
    fun `parser failure falls back to defaultValue`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf("user_plan" to "unknown")
        )
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "user_plan",
            defaultValue = "Free",
            parser = { raw -> if (raw == "Premium") "Premium" else "Free" }
        )

        manager.value.test {
            assertEquals("Free", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
