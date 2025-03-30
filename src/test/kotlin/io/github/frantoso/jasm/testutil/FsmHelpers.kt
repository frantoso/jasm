package io.github.frantoso.jasm.testutil

import io.github.frantoso.jasm.EndState
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.IEvent
import io.github.frantoso.jasm.State
import org.assertj.core.api.Assertions.assertThat

class TestData(
    val startState: State,
    val event: IEvent,
    val endState: EndState,
    val wasHandled: Boolean,
)

fun testStateChange(
    fsm: Fsm,
    testData: List<TestData>,
) {
    val debugInterface = fsm.debugInterface

    testData.forEach {
        debugInterface.setState(it.startState)

        val handled = fsm.trigger(it.event)

        assertThat(fsm.currentState).isEqualTo(it.endState)
        assertThat(handled).isEqualTo(it.wasHandled)

        if (it.endState is FinalState) return@forEach
    }
}
