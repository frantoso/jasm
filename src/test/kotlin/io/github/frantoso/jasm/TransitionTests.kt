package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test

class TransitionTests {
    data class TestData(
        val event: IEvent,
        val returnOfGuard: Boolean,
        val expected: Boolean,
    )

    @Nested
    inner class TransitionTest {
        @Test
        fun `initialization via standard constructor`() {
            val transition = Transition(StartEvent::class, TransitionEndPoint(State("abc"))) { true }

            assertThat(transition.eventType).isEqualTo(StartEvent::class)
            assertThat(transition.endPoint.state.name).isEqualTo("abc")
        }

        @Test
        fun `initialization via alternative constructor`() {
            val transition = Transition(StartEvent::class, State("abc")) { true }

            assertThat(transition.eventType).isEqualTo(StartEvent::class)
            assertThat(transition.endPoint.state.name).isEqualTo("abc")
        }

        @Test
        fun isToFinal() {
            val transition1 = Transition(StartEvent::class, State("abc")) { true }
            val transition2 = Transition(StartEvent::class, FinalState()) { true }

            assertThat(transition1.isToFinal).isFalse
            assertThat(transition2.isToFinal).isTrue
        }

        @Test
        fun getCondition() {
            val transition = Transition(StartEvent::class, State("abc")) { true }

            assertThat(transition.guard()).isTrue
        }

        @Test
        fun getEndPoint() {
            val transition = Transition(StartEvent::class, State("abc")) { true }

            assertThat(transition.endPoint.state.name).isEqualTo("abc")
            assertThat(transition.endPoint.history.isHistory).isFalse
            assertThat(transition.endPoint.history.isDeepHistory).isFalse
        }

        @Test
        fun `uses default construction with history`() {
            val transition = Transition(StartEvent::class, State("abc").history) { true }

            assertThat(transition.endPoint.state.name).isEqualTo("abc")
            assertThat(transition.endPoint.history.isHistory).isTrue
            assertThat(transition.endPoint.history.isDeepHistory).isFalse
        }

        @TestFactory
        fun `tests whether a transition is allowed`() =
            listOf(
                TestData(event = StartEvent, returnOfGuard = true, expected = true),
                TestData(event = StartEvent, returnOfGuard = false, expected = false),
                TestData(event = NoEvent, returnOfGuard = true, expected = false),
                TestData(event = NoEvent, returnOfGuard = false, expected = false),
            ).mapIndexed { index, data ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - isAllowed should return ${data.expected}") {
                    val transition = Transition(StartEvent::class, State("abc")) { data.returnOfGuard }

                    assertThat(transition.isAllowed(data.event)).isEqualTo(data.expected)
                }
            }
    }

    @Nested
    inner class DataTransitionTest {
        @Test
        fun `initialization via standard constructor`() {
            val transition = DataTransition(StartEvent::class, Int::class, TransitionEndPoint(State("abc"))) { true }

            assertThat(transition.eventType).isEqualTo(StartEvent::class)
            assertThat(transition.endPoint.state.name).isEqualTo("abc")
        }

        @Test
        fun `initialization via alternative constructor`() {
            val transition = DataTransition(StartEvent::class, Int::class, State("abc")) { true }

            assertThat(transition.eventType).isEqualTo(StartEvent::class)
            assertThat(transition.endPoint.state.name).isEqualTo("abc")
        }

        @Test
        fun isToFinal() {
            val transition1 = DataTransition(StartEvent::class, Int::class, State("abc")) { true }
            val transition2 = DataTransition(StartEvent::class, Int::class, FinalState()) { true }

            assertThat(transition1.isToFinal).isFalse
            assertThat(transition2.isToFinal).isTrue
        }

        @Test
        fun getCondition() {
            val transition =
                DataTransition(StartEvent::class, Int::class, TransitionEndPoint(State("abc"))) { i -> i!! < 20 }
            assertThat(transition.guard(20)).isFalse
            assertThat(transition.guard(19)).isTrue
        }

        @Test
        fun `uses default construction with history`() {
            val transition = DataTransition(StartEvent::class, Int::class, State("abc").history) { true }

            assertThat(transition.endPoint.state.name).isEqualTo("abc")
            assertThat(transition.endPoint.history.isHistory).isTrue
            assertThat(transition.endPoint.history.isDeepHistory).isFalse
        }

        @TestFactory
        fun `tests whether a transition is allowed`() =
            listOf(
                TestData(event = dataEvent<StartEvent, Int>(1), returnOfGuard = true, expected = true),
                TestData(event = dataEvent<StartEvent, Double>(1.0), returnOfGuard = true, expected = false),
                TestData(event = dataEvent<StartEvent, Int>(1), returnOfGuard = false, expected = false),
                TestData(event = StartEvent, returnOfGuard = true, expected = false),
                TestData(event = StartEvent, returnOfGuard = false, expected = false),
                TestData(event = NoEvent, returnOfGuard = true, expected = false),
                TestData(event = NoEvent, returnOfGuard = false, expected = false),
            ).mapIndexed { index, data ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - isAllowed should return ${data.expected}") {
                    val transition = DataTransition(StartEvent::class, Int::class, State("abc")) { data.returnOfGuard }

                    assertThat(transition.isAllowed(data.event)).isEqualTo(data.expected)
                }
            }
    }
}
