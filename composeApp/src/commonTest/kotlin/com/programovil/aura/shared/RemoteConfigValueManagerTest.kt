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

    @Test
    fun `initialize seeds value from current RemoteConfigService string`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf("user_plan" to "Premium")
        )
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "user_plan",
            defaultValue = "Free",
            parser = { raw -> if (raw == "Premium") "Premium" else "Free" }
        )

        manager.initialize()

        assertEquals("Premium", manager.value.value)
    }

    @Test
    fun `stop cancels the internal scope - refresh becomes a no-op`() = runTest {
        var callCount = 0
        val fake = object : RemoteConfigService {
            override suspend fun getBoolean(key: String, default: Boolean) = default
            override suspend fun getString(key: String, default: String) = {
                callCount++
                default
            }
            override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
            override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
        }
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "k",
            defaultValue = "d",
            parser = { it }
        )

        manager.stop()
        manager.refresh()

        assertEquals(0, callCount)
    }

    @Test
    fun `refresh parses the latest string after a real-time update`() = runTest {
        val backing = mutableMapOf<String, String>("k" to "v1")
        var listener: (() -> Unit)? = null
        val fake = object : RemoteConfigService {
            override suspend fun getBoolean(key: String, default: Boolean) = default
            override suspend fun getString(key: String, default: String) = backing[key] ?: default
            override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
            override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) { listener = onUpdate }
        }
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "k",
            defaultValue = "v0",
            parser = { it }
        )
        manager.initialize()
        assertEquals("v1", manager.value.value)

        backing["k"] = "v2"
        listener?.invoke()

        manager.value.test {
            assertEquals("v2", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
