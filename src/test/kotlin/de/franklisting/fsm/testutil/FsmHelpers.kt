package de.franklisting.fsm.testutil

import de.franklisting.fsm.EndState
import de.franklisting.fsm.Event
import de.franklisting.fsm.FinalState
import de.franklisting.fsm.Fsm
import de.franklisting.fsm.State
import org.assertj.core.api.Assertions.assertThat

class TestData<T>(
    val startState: State,
    val event: Event,
    val data: T,
    val endState: EndState,
    val wasHandled: Boolean,
)

fun <T> testStateChange(
    fsm: Fsm<T>,
    testData: List<TestData<T>>,
) {
    val debugInterface = fsm.debugInterface

    testData.forEach {
        debugInterface.setState(it.startState)

        val handled = fsm.trigger(it.event, it.data)

        assertThat(fsm.currentState.state).isEqualTo(it.endState)
        assertThat(handled).isEqualTo(it.wasHandled)

        if (it.endState is FinalState) return@forEach
    }
}
