package io.github.frantoso.jasm.testutil

import io.github.frantoso.jasm.EndState
import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.State
import org.assertj.core.api.Assertions.assertThat

class TestData<T>(
    val startState: State,
    val event: Event,
    val endState: EndState,
    val wasHandled: Boolean,
)

fun <T> testStateChange(
    fsm: Fsm,
    testData: List<TestData<T>>,
) {
    val debugInterface = fsm.debugInterface

    testData.forEach {
        debugInterface.setState(it.startState)

        val handled = fsm.trigger(it.event)

        assertThat(fsm.currentState.state).isEqualTo(it.endState)
        assertThat(handled).isEqualTo(it.wasHandled)

        if (it.endState is FinalState) return@forEach
    }
}
