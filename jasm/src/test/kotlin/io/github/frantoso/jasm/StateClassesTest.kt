package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test

class StateClassesTest {
    object StateNo1 : State()

    @Nested
    inner class StateTest {
        @Test
        fun `verify normal state`() {
            val state = State(TEST_STATE_1_NAME)

            assertThat(state.name).isEqualTo(TEST_STATE_1_NAME)
            assertThat("$state").isEqualTo(TEST_STATE_1_NAME)
            assertThat(state.id).contains("State_0")
        }

        @Test
        fun `gets the end point for history`() {
            val state = State(TEST_STATE_1_NAME)

            val endPoint = state.history

            assertThat(endPoint.history.isHistory).isTrue
            assertThat(endPoint.history.isDeepHistory).isFalse
            assertThat(endPoint.state).isSameAs(state)
        }

        @Test
        fun `gets the end point for deep history`() {
            val state = State(TEST_STATE_1_NAME)

            val endPoint = state.deepHistory

            assertThat(endPoint.history.isHistory).isFalse
            assertThat(endPoint.history.isDeepHistory).isTrue
            assertThat(endPoint.state).isSameAs(state)
        }

        @TestFactory
        fun `tests whether it creates the right name`() =
            listOf(
                State("test-state-1") to "test-state-1",
                State("") to "State",
                State(" ") to "State",
                State("  ") to "State",
                State("test-state-2") to "test-state-2",
                StateNo1 to "StateNo1",
            ).mapIndexed { index, (state, expected) ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - name should be $expected") {
                    assertThat(state.name).isEqualTo(expected)
                }
            }
    }

    @Nested
    inner class InitialStateTest {
        @Test
        fun `verify initial state`() {
            val state = InitialState()

            assertThat(state.name).isEqualTo("Initial")
        }
    }

    @Nested
    inner class FinalStateTest {
        @Test
        fun `verify final state`() {
            val state = FinalState()

            assertThat(state.name).isEqualTo("Final")
        }

        @TestFactory
        fun `equals returns true for different objects of FinalState`() =
            listOf(
                FinalState() to true,
                InitialState() to false,
                null to false,
            ).mapIndexed { index, (toCompare, expected) ->
                DynamicTest.dynamicTest("${"%02d".format(index)} - equals should return $expected") {
                    val state = FinalState()

                    assertThat(state == toCompare).isEqualTo(expected)
                    assertThat(state.hashCode() == toCompare?.hashCode()).isEqualTo(expected)
                }
            }
    }

    companion object {
        private const val TEST_STATE_1_NAME = "test-state-1"
    }
}
