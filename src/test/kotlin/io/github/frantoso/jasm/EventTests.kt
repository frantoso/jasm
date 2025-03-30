package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test

class EventTests {
    object TestEvent : Event()

    class TestEventClass : Event()

    @Nested
    inner class EventTest {
        @Test
        fun `toString shows the simple class name`() {
            assertThat(TestEvent.toString()).isEqualTo("TestEvent")
        }

        @Test
        fun getNoEvent() {
            val event1 = NoEvent
            val event2 = NoEvent

            assertThat(event1).isSameAs(event2)
            assertThat(event1.type).isEqualTo(NoEvent::class)
        }

        @Test
        fun getStartEvent() {
            val event1 = StartEvent
            val event2 = StartEvent

            assertThat(event1).isSameAs(event2)
            assertThat(event1.type).isEqualTo(StartEvent::class)
        }

        @Test
        fun `anonymous objects have the name Event`() {
            val event1 = object : Event() {}

            assertThat(event1.toString()).isEqualTo("Event")
        }

        @TestFactory
        fun `two Event objects of the same type are equal`() =
            listOf(
                TestEventClass() to true,
                TestEvent to false,
                null to false,
            ).mapIndexed { index, (other, expected) ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - equals should return $expected") {
                    val event1 = TestEventClass()

                    assertThat(event1 == other).isEqualTo(expected)
                }
            }

        @Test
        fun `two Event objects of the same type have the same hash code`() {
            val event1 = TestEventClass()
            val event2 = TestEventClass()

            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }
    }

    @Nested
    inner class DataEventTest {
        @Test
        fun `creates a data event`() {
            val event = DataEvent(42, TestEvent::class)

            assertThat(event.type).isEqualTo(TestEvent::class)
        }

        @Test
        fun `copies the data`() {
            val event = DataEvent(42, TestEvent::class)
            val copiedEvent = event.fromData(TestEventClass::class)

            assertThat(copiedEvent.type).isEqualTo(TestEventClass::class)
            assertThat(copiedEvent.data).isEqualTo(event.data)
        }

        @Test
        fun `creates a data event with helper function`() {
            val event = dataEvent<TestEvent, Int>(42)

            assertThat(event.type).isEqualTo(TestEvent::class)
            assertThat(event.data).isEqualTo(42)
        }

        @Test
        fun `toString shows the simple class name of the encapsulated event`() {
            val event = dataEvent<TestEvent, Int>(42)

            assertThat(event.toString()).isEqualTo("TestEvent")
        }

        @Test
        fun `anonymous objects have the name Event`() {
            val anonymous = object : Event() {}
            val event = DataEvent(42, anonymous::class)

            assertThat(event.toString()).isEqualTo("Event")
        }
    }
}
