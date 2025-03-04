package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class EventTest {
    object TestEvent : Event()

    @Test
    fun getName() {
        assertThat(TestEvent.name).isEqualTo("TestEvent")
    }

    @Test
    fun getNoEvent() {
        val event1 = NoEvent
        val event2 = NoEvent

        assertThat(event1).isSameAs(event2)
        assertThat(event1.name).isEqualTo("NoEvent")
    }

    @Test
    fun getStartEvent() {
        val event1 = StartEvent
        val event2 = StartEvent

        assertThat(event1).isSameAs(event2)
        assertThat(event1.name).isEqualTo("StartEvent")
    }
}
