package de.franklisting.fsm.testutil

import de.franklisting.fsm.Event
import de.franklisting.fsm.Fsm
import de.franklisting.fsm.State
import org.assertj.core.api.Assertions.assertThat

class TestData<T>(
    val startState: State<T>,
    val event: Event,
    val data: T,
    val endState: State<T>,
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

        assertThat(fsm.currentState).isEqualTo(it.endState)
        assertThat(handled).isEqualTo(it.wasHandled)

        if (it.endState == fsm.final) return@forEach
    }
}
