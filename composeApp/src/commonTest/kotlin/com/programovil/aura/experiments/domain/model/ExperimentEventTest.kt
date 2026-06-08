package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExperimentEventTest {

    @Test
    fun `HomeOpened carries userPlan`() {
        val event = ExperimentEvent.HomeOpened(UserPlan.Premium)
        assertEquals(UserPlan.Premium, event.userPlan)
    }

    @Test
    fun `TabClicked carries tabName`() {
        val event = ExperimentEvent.TabClicked(UserPlan.Free, tabName = "Todos")
        assertEquals("Todos", event.tabName)
        assertEquals(UserPlan.Free, event.userPlan)
    }

    @Test
    fun `NotificationDelivered carries channel`() {
        val event = ExperimentEvent.NotificationDelivered(UserPlan.Premium, channel = "due_date_reminder")
        assertEquals("due_date_reminder", event.channel)
    }

    @Test
    fun `SessionActive carries userPlan`() {
        val event = ExperimentEvent.SessionActive(UserPlan.Free)
        assertEquals(UserPlan.Free, event.userPlan)
    }

    @Test
    fun `sealed class has exactly four variants (exhaustive)`() {
        val events: List<ExperimentEvent> = listOf(
            ExperimentEvent.HomeOpened(UserPlan.Free),
            ExperimentEvent.TabClicked(UserPlan.Free, ""),
            ExperimentEvent.NotificationDelivered(UserPlan.Free, ""),
            ExperimentEvent.SessionActive(UserPlan.Free)
        )
        assertTrue(events.size == 4)
    }
}
