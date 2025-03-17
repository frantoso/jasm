package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class EventTest {
    object TestEvent : Event()

    object NamedTestEvent : Event("Boom")

    @Test
    fun `if name is not set it has the class name`() {
        assertThat(TestEvent.name).isEqualTo("TestEvent")
    }

    @Test
    fun `name returns the right string`() {
        assertThat(NamedTestEvent.name).isEqualTo("Boom")
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

    @Test
    fun `toString gets the name`() {
        assertThat(TestEvent.toString()).isEqualTo("TestEvent")
    }
}
