package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ActionTests {
    @Nested
    inner class ActionTest {
        @Test
        fun `executes the action`() {
            var counter = 0
            val action = Action { ++counter }

            action.fire(StartEvent)

            assertThat(counter).isEqualTo(1)
        }

        @Test
        fun `executes an invalid action and throws an FsmException`() {
            var counter = 0
            val action = Action { counter = 4 / counter }

            assertThatThrownBy { action.fire(StartEvent) }.isInstanceOf(FsmException::class.java)
        }
    }

    @Nested
    inner class DataActionTest {
        @Test
        fun `executes the action`() {
            var counter = 1
            val action = DataAction(Int::class) { data -> if (data != null) counter += data }

            action.fire(dataEvent<StartEvent, Int>(3))

            assertThat(counter).isEqualTo(4)
        }

        @Test
        fun `executes the action with null as parameter (no data event)`() {
            var counter = 1
            var dataProvided: Any? = 2
            val action =
                DataAction(Int::class) { data ->
                    dataProvided = data
                    if (data != null) counter += data
                }

            action.fire(StartEvent)

            assertThat(counter).isEqualTo(1)
            assertThat(dataProvided).isNull()
        }

        @Test
        fun `executes the action with null as parameter (wrong data type)`() {
            var counter = 1
            var dataProvided: Any? = 2
            val action =
                DataAction(Int::class) { data ->
                    dataProvided = data
                    if (data != null) counter += data
                }

            action.fire(dataEvent<StartEvent, Double>(3.2))

            assertThat(counter).isEqualTo(1)
            assertThat(dataProvided).isNull()
        }

        @Test
        fun `executes an invalid action and throws an FsmException`() {
            var counter = 0
            val action = DataAction(Int::class) { counter = 4 / counter }

            assertThatThrownBy { action.fire(StartEvent) }.isInstanceOf(FsmException::class.java)
        }
    }
}
