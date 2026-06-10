package com.programovil.aura.shared

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.locks.LockSupport
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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

        manager.value.test {
            assertEquals("Premium", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stop cancels the internal scope - refresh becomes a no-op`() = runTest {
        var callCount = 0
        val fake = object : RemoteConfigService {
            override suspend fun getBoolean(key: String, default: Boolean) = default
            override suspend fun getString(key: String, default: String): String {
                callCount++
                return default
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
    fun `refresh parses the latest string after a real-time update`() = runTest(UnconfinedTestDispatcher()) {
        val backing = mutableMapOf<String, String>("k" to "v1")
        val fake = object : RemoteConfigService {
            override suspend fun getBoolean(key: String, default: Boolean) = default
            override suspend fun getString(key: String, default: String) = backing[key] ?: default
            override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
            override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
        }
        val manager = RemoteConfigValueManager(
            remoteConfigService = fake,
            key = "k",
            defaultValue = "v0",
            parser = { it }
        )
        manager.refresh()
        for (i in 1..30) {
            delay(50)
            LockSupport.parkNanos(1_000_000)
            if (manager.value.value == "v1") break
        }

        manager.value.test {
            assertEquals("v1", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        backing["k"] = "v2"
        manager.refresh()
        for (i in 1..30) {
            delay(50)
            LockSupport.parkNanos(1_000_000)
            if (manager.value.value == "v2") break
        }

        manager.value.test {
            assertEquals("v2", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
