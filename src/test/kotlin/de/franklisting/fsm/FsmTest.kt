package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class FsmTest {
    private object Event1 : Event()

    private lateinit var state1: State<Int>
    private lateinit var state2: State<Int>
    private lateinit var fsm: FsmSync<Int>

    @BeforeEach
    fun createFsm() {
        fsm = FsmSync("fsm")
        state1 = State("first")
        state2 = State("second")
        fsm.initialTransition(state1)
        state1.transition(Event1, state2)
        state2.transition(Event1, fsm.final)
    }

    @Test
    fun `creates a new state machine`() {
        val fsm = FsmSync<Int>("myFsm")
        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState.isInitial).isTrue
        assertThat(fsm.currentState.isFinal).isFalse
        assertThat(fsm.name).isEqualTo("myFsm")
    }

    @Test
    fun `starts the state machine`() {
        assertThat(fsm.isRunning).isFalse

        fsm.start(42)

        assertThat(fsm.isRunning).isTrue
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState).isSameAs(state1)
        assertThat(fsm.currentState.isInitial).isFalse
        assertThat(fsm.currentState.isFinal).isFalse
    }

    @Test
    fun `throws an exception when adding a second initial transition`() {
        assertThatThrownBy { fsm.initialTransition(state2) }.isInstanceOf(FsmException::class.java)
    }

    @Test
    fun `trigger changes to the next state`() {
        fsm.start(42)

        fsm.trigger(Event1, 23)

        assertThat(fsm.isRunning).isTrue
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState).isSameAs(state2)
        assertThat(fsm.currentState.isInitial).isFalse
        assertThat(fsm.currentState.isFinal).isFalse
    }

    @Test
    fun `trigger to final stops the state machine`() {
        fsm.start(42)
        fsm.trigger(Event1, 23)

        fsm.trigger(Event1, 23)

        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isTrue
        assertThat(fsm.currentState).isSameAs(fsm.final)
        assertThat(fsm.currentState.isInitial).isFalse
        assertThat(fsm.currentState.isFinal).isTrue
    }

    @Test
    fun `using predefined NoEvent on normal transition throws an exception`() {
        fsm.start(42)

        assertThatThrownBy { fsm.trigger(NoEvent, 23) }.isInstanceOf(FsmException::class.java)
    }

    @Test
    fun `doAction triggers the action in state`() {
        var result = 0
        state1.doInState { i -> result = i }
        fsm.start(42)

        fsm.doAction(22)

        assertThat(result).isEqualTo(22)
    }

    @Test
    fun `using InvalidState as end point throws an exception`() {
        val fsm = FsmSync<Int>("fsm")
        val state1 = InvalidState<Int>()

        assertThatThrownBy { fsm.initialTransition(state1) }.isInstanceOf(FsmException::class.java)
        assertThatThrownBy { state1.transition(Event1, state1) }.isInstanceOf(FsmException::class.java)
    }
}
