package io.github.frantoso.jasm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class StateContainerExtensionTests {
    private object TestEvent : Event()

    @Test
    fun `adds a child`() {
        val fsm =
            fsmOf(
                FSM_NAME,
                State("Start")
                    .toContainer(),
            )
        val container = State(TEST_STATE_1).child(fsm)

        assertThat(container.hasTransitions).isFalse
        assertThat(container.hasChildren).isTrue
        assertThat(container.debugInterface.childDump).hasSize(1)
    }

    @Test
    fun `adds a list of children`() {
        val fsm1 =
            fsmOf(
                FSM_NAME,
                State("Start")
                    .toContainer(),
            )
        val fsm2 =
            fsmOf(
                "Otto",
                State("Start")
                    .toContainer(),
            )
        val container =
            State(TEST_STATE_1)
                .children(listOf(fsm1, fsm2))

        assertThat(container.hasTransitions).isFalse
        assertThat(container.hasChildren).isTrue
        assertThat(container.debugInterface.childDump).hasSize(2)
    }

    @Test
    fun `adds a transition to a state`() {
        val container1 = State(TEST_STATE_1).transition<TestEvent>(State(TEST_STATE_2))
        val container2 = State(TEST_STATE_1).transition<TestEvent>(State(TEST_STATE_2)) { true }
        val container3 = State(TEST_STATE_1).transition<TestEvent>(State(TEST_STATE_2)) { false }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0].isAllowed(TestEvent)).isTrue
        assertThat(container2.transitions[0].isAllowed(TestEvent)).isTrue
        assertThat(container3.transitions[0].isAllowed(TestEvent)).isFalse
    }

    @Test
    fun `adds a transition to an endpoint`() {
        val container1 = State(TEST_STATE_1).transition<TestEvent>(State(TEST_STATE_2).history)
        val container2 = State(TEST_STATE_1).transition<TestEvent>(State(TEST_STATE_2).history) { true }
        val container3 = State(TEST_STATE_1).transition<TestEvent>(State(TEST_STATE_2).history) { false }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0].isAllowed(TestEvent)).isTrue
        assertThat(container2.transitions[0].isAllowed(TestEvent)).isTrue
        assertThat(container3.transitions[0].isAllowed(TestEvent)).isFalse
    }

    @Test
    fun `adds a transition to a state and parameter-guard`() {
        val container1 = State(TEST_STATE_1).transition<TestEvent, Int>(State(TEST_STATE_2))
        val container2 = State(TEST_STATE_1).transition<TestEvent, Int>(State(TEST_STATE_2)) { data -> data == 1 }
        val container3 = State(TEST_STATE_1).transition<TestEvent, Int>(State(TEST_STATE_2)) { data -> data == 1 }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0]).isInstanceOf(DataTransition::class.java)
        assertThat(container1.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
        assertThat(container2.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
        assertThat(container3.transitions[0].isAllowed(DataEvent(2, TestEvent::class))).isFalse
    }

    @Test
    fun `adds a transition to an endpoint and parameter-guard`() {
        val container1 = State(TEST_STATE_1).transition<TestEvent, Int>(State(TEST_STATE_2).history)
        val container2 =
            State(TEST_STATE_1).transition<TestEvent, Int>(State(TEST_STATE_2).history) { data -> data == 1 }
        val container3 =
            State(TEST_STATE_1).transition<TestEvent, Int>(State(TEST_STATE_2).history) { data -> data == 1 }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0]).isInstanceOf(DataTransition::class.java)
        assertThat(container1.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
        assertThat(container2.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
        assertThat(container3.transitions[0].isAllowed(DataEvent(2, TestEvent::class))).isFalse
    }

    @Test
    fun `adds a transition without event to a nested state`() {
        val fsm = fsmOf("fsm", State("Start").toContainer())
        val container =
            State(TEST_STATE_1)
                .child(fsm)
                .transitionWithoutEvent<Int>(State(TEST_STATE_2).history)

        assertThat(container.hasTransitions).isTrue
        assertThat(container.hasChildren).isTrue
        assertThat(
            container.debugInterface.transitionDump
                .first()
                .eventType,
        ).isEqualTo(NoEvent::class)
    }

    @Test
    fun `adds a transition without event`() {
        val container1 = State(TEST_STATE_1).transitionWithoutEvent(State(TEST_STATE_2))
        val container2 = State(TEST_STATE_1).transitionWithoutEvent(State(TEST_STATE_2)) { true }
        val container3 = State(TEST_STATE_1).transitionWithoutEvent(State(TEST_STATE_2)) { false }
        val container4 = State(TEST_STATE_1).transitionWithoutEvent(State(TEST_STATE_2).history)
        val container5 = State(TEST_STATE_1).transitionWithoutEvent(State(TEST_STATE_2).history) { true }
        val container6 = State(TEST_STATE_1).transitionWithoutEvent(State(TEST_STATE_2).history) { false }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0].isAllowed(NoEvent)).isTrue
        assertThat(container2.transitions[0].isAllowed(NoEvent)).isTrue
        assertThat(container3.transitions[0].isAllowed(NoEvent)).isFalse
        assertThat(container4.transitions[0].isAllowed(NoEvent)).isTrue
        assertThat(container5.transitions[0].isAllowed(NoEvent)).isTrue
        assertThat(container6.transitions[0].isAllowed(NoEvent)).isFalse
    }

    @Test
    fun `adds a transition without event and parameter-guard`() {
        val container1 = State(TEST_STATE_1).transitionWithoutEvent<Int>(State(TEST_STATE_2))
        val container2 = State(TEST_STATE_1).transitionWithoutEvent<Int>(State(TEST_STATE_2)) { data -> data == 1 }
        val container3 = State(TEST_STATE_1).transitionWithoutEvent<Int>(State(TEST_STATE_2)) { data -> data == 1 }
        val container4 = State(TEST_STATE_1).transitionWithoutEvent<Int>(State(TEST_STATE_2).history)
        val container5 =
            State(TEST_STATE_1).transitionWithoutEvent<Int>(State(TEST_STATE_2).history) { data -> data == 1 }
        val container6 =
            State(TEST_STATE_1).transitionWithoutEvent<Int>(State(TEST_STATE_2).history) { data -> data == 1 }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0]).isInstanceOf(DataTransition::class.java)
        assertThat(container1.transitions[0].isAllowed(DataEvent(1, NoEvent::class))).isTrue
        assertThat(container2.transitions[0].isAllowed(DataEvent(1, NoEvent::class))).isTrue
        assertThat(container3.transitions[0].isAllowed(DataEvent(2, NoEvent::class))).isFalse
        assertThat(container4.transitions[0].isAllowed(DataEvent(1, NoEvent::class))).isTrue
        assertThat(container5.transitions[0].isAllowed(DataEvent(1, NoEvent::class))).isTrue
        assertThat(container6.transitions[0].isAllowed(DataEvent(2, NoEvent::class))).isFalse
    }

    @Test
    fun `adds a transition to final state`() {
        val container1 = State(TEST_STATE_1).transitionToFinal<TestEvent>()
        val container2 = State(TEST_STATE_1).transitionToFinal<TestEvent> { true }
        val container3 = State(TEST_STATE_1).transitionToFinal<TestEvent> { false }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0].isAllowed(TestEvent)).isTrue
        assertThat(container2.transitions[0].isAllowed(TestEvent)).isTrue
        assertThat(container3.transitions[0].isAllowed(TestEvent)).isFalse
    }

    @Test
    fun `adds a transition to final state with parameter-guard`() {
        val container1 = State(TEST_STATE_1).transitionToFinal<TestEvent, Int>()
        val container2 = State(TEST_STATE_1).transitionToFinal<TestEvent, Int> { data -> data == 1 }
        val container3 = State(TEST_STATE_1).transitionToFinal<TestEvent, Int> { data -> data == 1 }

        assertThat(container1.hasTransitions).isTrue
        assertThat(container1.transitions[0]).isInstanceOf(DataTransition::class.java)
        assertThat(container1.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
        assertThat(container2.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
        assertThat(container3.transitions[0].isAllowed(DataEvent(2, TestEvent::class))).isFalse
    }

    @Test
    fun `adds an entry action with parameter`() {
        var counter = 5
        val container = State(TEST_STATE_1).entry<Int> { data -> counter += data!! }

        container.onEntry.fire(dataEvent<NoEvent, Int>(10))

        assertThat(counter).isEqualTo(15)
    }

    @Test
    fun `adds an entry action`() {
        var counter = 5
        val container = State(TEST_STATE_1).entry { counter = 42 }

        container.onEntry.fire(NoEvent)

        assertThat(counter).isEqualTo(42)
    }

    @Test
    fun `adds an exit action with parameter`() {
        var counter = 2
        val container = State(TEST_STATE_1).exit<Int> { data -> counter += data!! }

        container.onExit.fire(dataEvent<NoEvent, Int>(19))

        assertThat(counter).isEqualTo(21)
    }

    @Test
    fun `adds an exit action`() {
        var counter = 2
        val container = State(TEST_STATE_1).exit { counter = 42 }

        container.onExit.fire(NoEvent)

        assertThat(counter).isEqualTo(42)
    }

    @Test
    fun `adds a do action with parameter`() {
        var counter = 2
        val container = State(TEST_STATE_1).doInState<Int> { data -> counter += data!! }

        container.onDoInState.fire(dataEvent<NoEvent, Int>(19))

        assertThat(counter).isEqualTo(21)
    }

    @Test
    fun `adds a do action`() {
        var counter = 2
        val container = State(TEST_STATE_1).doInState { counter = 42 }

        container.onDoInState.fire(NoEvent)

        assertThat(counter).isEqualTo(42)
    }

    companion object {
        private const val TEST_STATE_1 = "test-state-1"
        private const val TEST_STATE_2 = "test-state-2"
        private const val FSM_NAME = "fsm"
    }
}
