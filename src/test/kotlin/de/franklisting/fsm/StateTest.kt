package de.franklisting.fsm

import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class StateTest {
    private object TestEvent : Event()

    private object StopEvent : Event()

//    private fun getStateWithChild(): State<Int> {
//        val state = spyk(State<Int>(TEST_STATE_1_NAME), recordPrivateCalls = true)
//        val fsm = FsmSync<Int>(FSM_NAME)
//        state.child(fsm)
//        state.start(42, History.None)
//
//        return state
//    }

    @Test
    fun `verify normal state`() {
        val state = State(TEST_STATE_1_NAME)

        assertThat(state.name).isEqualTo(TEST_STATE_1_NAME)
        assertThat("$state").isEqualTo(TEST_STATE_1_NAME)
    }

    @Test
    fun `verify initial state`() {
        val state = InitialState()

        assertThat(state.name).isEqualTo("Initial")
    }

    @Test
    fun `verify final state`() {
        val state = FinalState()

        assertThat(state.name).isEqualTo("Final")
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

    @Test
    fun `raw state does not have a transition`() {
        val state = State(TEST_STATE_1_NAME)

//        assertThat(state.hasTransitions).isFalse
    }

    @Test
    fun `a state with transition if flagged`() {
        val startState = State(TEST_STATE_1_NAME)
        val endState = State(TEST_STATE_1_NAME)

        startState.transition<Int>(TestEvent, endState).transition(StopEvent, FinalState())

//        assertThat(startState.hasTransitions).isTrue
    }

    @Test
    fun `starting a normal state with history ends in a normal start (calls entry)`() {
        val stateContainer = spyk(State(TEST_STATE_1_NAME).toContainer<Int>(), recordPrivateCalls = true)

        stateContainer.start(1, History.H)

        verify(exactly = 1) {
            stateContainer["tryStartHistory"](any())
            stateContainer["fireOnEntry"](1)
            stateContainer["startChildren"](1)
        }

        verify(exactly = 0) {
            stateContainer["tryStartDeepHistory"]()
        }
    }

    @Test
    fun `starting a parent state with history does not call entry`() {
//        val state = spyk(getStateWithChild(), recordPrivateCalls = true)
//
//        state.start(1, History.H)
//
//        verify(exactly = 1) {
//            state["tryStartHistory"](1)
//        }
//
//        verify(exactly = 0) {
//            state["tryStartDeepHistory"]()
//            state["fireOnEntry"](any())
//            state["startChildren"](any())
//        }
    }

    @Test
    fun `starting a normal state with deep history ends in a normal start (calls entry)`() {
//        val state = spyk(State<Int>(TEST_STATE_1_NAME), recordPrivateCalls = true)
//
//        state.start(1, History.Hd)
//
//        verify(exactly = 1) {
//            state["fireOnEntry"](1)
//            state["startChildren"](1)
//        }
    }

    @Test
    fun `starting a parent state with deep history does not call entry`() {
//        val state = spyk(getStateWithChild(), recordPrivateCalls = true)
//
//        state.start(1, History.Hd)
//
//        verify(exactly = 1) {
//            state["tryStartDeepHistory"]()
//        }
//
//        verify(exactly = 0) {
//            state["tryStartHistory"](any())
//            state["fireOnEntry"](any())
//            state["startChildren"](any())
//        }
    }

    @Test
    fun `fires on entry`() {
//        val state = spyk(State<Int>(TEST_STATE_1_NAME), recordPrivateCalls = true)
//
//        state.start(1, History.None)
//
//        verify(exactly = 1) {
//            state["fireOnEntry"](1)
//        }
    }

    @Test
    fun `fires on exit`() {
//        val state = spyk(State<Int>(TEST_STATE_1_NAME), recordPrivateCalls = true)
//        val nextState = spyk(State<Int>(TEST_STATE_2_NAME), recordPrivateCalls = true)
//        state.transition(TestEvent, nextState)
//
//        state.trigger(TestEvent, 42)
//
//        verify(exactly = 1) {
//            state["fireOnExit"](42)
//        }
    }

    class TestHelper {
        fun doSomething(data: Int) = println(data)
    }

    @Test
    fun doInState() {
//        val helper = spyk(TestHelper())
//        val state = State<Int>(TEST_STATE_1_NAME)
//        state.doInState(helper::doSomething)
//
//        state.fireDoInState(42)
//
//        verify(exactly = 1) {
//            helper.doSomething(42)
//        }
    }

    @Test
    fun transition() {
    }

    @Test
    fun testTransition() {
    }

    @Test
    fun testTransition1() {
    }

    @Test
    fun testTransition2() {
    }

    @Test
    fun testToString() {
    }

    @Test
    fun fireDoInState() {
    }

    @Test
    fun `trigger$state_machine_kotlin`() {
    }

    @Test
    fun getName() {
    }

    companion object {
        private const val TEST_STATE_1_NAME = "test-state-1"
        private const val TEST_STATE_2_NAME = "test-state-2"
        private const val FSM_NAME = "fsm"
    }
}
