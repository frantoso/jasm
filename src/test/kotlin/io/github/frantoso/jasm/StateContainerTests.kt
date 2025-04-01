package io.github.frantoso.jasm

import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test

class StateContainerTests {
    private object TestEvent : Event()

    private object StopEvent : Event()

    private object FinishEvent : Event()

    private fun getContainerWithChild(): StateContainer {
        val fsm = fsmOf("fsmChild", State("Start").toContainer())
        val container = State(TEST_STATE_1_NAME).child(fsm)
        container.start()
        return container
    }

    private fun emptyContainer(state: State): StateContainer =
        StateContainer(
            state,
            emptyList(),
            emptyList(),
            NoAction,
            NoAction,
            NoAction,
        )

    @Nested
    inner class StateContainerTest {
        @Test
        fun `test initialization`() {
            val container = emptyContainer(State(TEST_STATE_1_NAME))

            assertThat(container.name).isEqualTo(TEST_STATE_1_NAME)
            assertThat(container.hasTransitions).isFalse
            assertThat(container.hasChildren).isFalse
        }

        @Test
        fun `adds a child`() {
            val fsm =
                fsmOf(
                    FSM_NAME,
                    State("Start")
                        .toContainer(),
                )
            val container = emptyContainer(State(TEST_STATE_1_NAME)).child(fsm)

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
                emptyContainer(State(TEST_STATE_1_NAME))
                    .children(listOf(fsm1, fsm2))

            assertThat(container.hasTransitions).isFalse
            assertThat(container.hasChildren).isTrue
            assertThat(container.debugInterface.childDump).hasSize(2)
        }

        @Test
        fun `adds a transition to a state`() {
            val container1 = emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent>(State(TEST_STATE_2_NAME))
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent>(State(TEST_STATE_2_NAME)) { true }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent>(State(TEST_STATE_2_NAME)) { false }

            assertThat(container1.hasTransitions).isTrue
            assertThat(container1.transitions[0].isAllowed(TestEvent)).isTrue
            assertThat(container2.transitions[0].isAllowed(TestEvent)).isTrue
            assertThat(container3.transitions[0].isAllowed(TestEvent)).isFalse
        }

        @Test
        fun `adds a transition to an endpoint`() {
            val container1 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent>(State(TEST_STATE_2_NAME).history)
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent>(State(TEST_STATE_2_NAME).history) { true }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent>(State(TEST_STATE_2_NAME).history) { false }

            assertThat(container1.hasTransitions).isTrue
            assertThat(container1.transitions[0].isAllowed(TestEvent)).isTrue
            assertThat(container2.transitions[0].isAllowed(TestEvent)).isTrue
            assertThat(container3.transitions[0].isAllowed(TestEvent)).isFalse
        }

        @Test
        fun `adds a transition to a state and parameter-guard`() {
            val container1 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent, Int>(State(TEST_STATE_2_NAME))
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent, Int>(State(TEST_STATE_2_NAME)) { data -> data == 1 }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent, Int>(State(TEST_STATE_2_NAME)) { data -> data == 1 }

            assertThat(container1.hasTransitions).isTrue
            assertThat(container1.transitions[0]).isInstanceOf(DataTransition::class.java)
            assertThat(container1.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
            assertThat(container2.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
            assertThat(container3.transitions[0].isAllowed(DataEvent(2, TestEvent::class))).isFalse
        }

        @Test
        fun `adds a transition to an endpoint and parameter-guard`() {
            val container1 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent, Int>(State(TEST_STATE_2_NAME).history)
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent, Int>(State(TEST_STATE_2_NAME).history) { data -> data == 1 }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transition<TestEvent, Int>(State(TEST_STATE_2_NAME).history) { data -> data == 1 }

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
                emptyContainer(State(TEST_STATE_1_NAME))
                    .child(fsm)
                    .transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME).history)

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
            val container1 = emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent(State(TEST_STATE_2_NAME))
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent(State(TEST_STATE_2_NAME)) { true }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent(State(TEST_STATE_2_NAME)) { false }
            val container4 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent(State(TEST_STATE_2_NAME).history)
            val container5 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent(State(TEST_STATE_2_NAME).history) { true }
            val container6 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent(State(TEST_STATE_2_NAME).history) { false }

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
            val container1 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME))
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME)) { data -> data == 1 }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME)) { data -> data == 1 }
            val container4 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME).history)
            val container5 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME).history) { data -> data == 1 }
            val container6 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionWithoutEvent<Int>(State(TEST_STATE_2_NAME).history) { data -> data == 1 }

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
            val container1 = emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal<TestEvent>()
            val container2 = emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal<TestEvent> { true }
            val container3 = emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal<TestEvent> { false }

            assertThat(container1.hasTransitions).isTrue
            assertThat(container1.transitions[0].isAllowed(TestEvent)).isTrue
            assertThat(container2.transitions[0].isAllowed(TestEvent)).isTrue
            assertThat(container3.transitions[0].isAllowed(TestEvent)).isFalse
        }

        @Test
        fun `adds a transition to final state with parameter-guard`() {
            val container1 = emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal<TestEvent, Int>()
            val container2 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal<TestEvent, Int> { data -> data == 1 }
            val container3 =
                emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal<TestEvent, Int> { data -> data == 1 }

            assertThat(container1.hasTransitions).isTrue
            assertThat(container1.transitions[0]).isInstanceOf(DataTransition::class.java)
            assertThat(container1.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
            assertThat(container2.transitions[0].isAllowed(DataEvent(1, TestEvent::class))).isTrue
            assertThat(container3.transitions[0].isAllowed(DataEvent(2, TestEvent::class))).isFalse
        }

        @Test
        fun `adds an entry action with IAction`() {
            var counter = 5
            val container =
                emptyContainer(State(TEST_STATE_1_NAME)).entry(DataAction(Int::class) { data -> counter += data!! })

            container.onEntry.fire(dataEvent<NoEvent, Int>(10))

            assertThat(counter).isEqualTo(15)
        }

        @Test
        fun `adds an entry action with parameter`() {
            var counter = 5
            val container = emptyContainer(State(TEST_STATE_1_NAME)).entry<Int> { data -> counter += data!! }

            container.onEntry.fire(dataEvent<NoEvent, Int>(10))

            assertThat(counter).isEqualTo(15)
        }

        @Test
        fun `adds an entry action`() {
            var counter = 5
            val container = emptyContainer(State(TEST_STATE_1_NAME)).entry { counter = 42 }

            container.onEntry.fire(NoEvent)

            assertThat(counter).isEqualTo(42)
        }

        @Test
        fun `adds an exit action with IAction`() {
            var counter = 5
            val container =
                emptyContainer(State(TEST_STATE_1_NAME)).exit(DataAction(Int::class) { data -> counter += data!! })

            container.onExit.fire(dataEvent<NoEvent, Int>(10))

            assertThat(counter).isEqualTo(15)
        }

        @Test
        fun `adds an exit action with parameter`() {
            var counter = 2
            val container = emptyContainer(State(TEST_STATE_1_NAME)).exit<Int> { data -> counter += data!! }

            container.onExit.fire(dataEvent<NoEvent, Int>(19))

            assertThat(counter).isEqualTo(21)
        }

        @Test
        fun `adds an exit action`() {
            var counter = 2
            val container = emptyContainer(State(TEST_STATE_1_NAME)).exit { counter = 42 }

            container.onExit.fire(NoEvent)

            assertThat(counter).isEqualTo(42)
        }

        @Test
        fun `adds a do action with IAction`() {
            var counter = 5
            val container =
                emptyContainer(State(TEST_STATE_1_NAME)).doInState(DataAction(Int::class) { data -> counter += data!! })

            container.onDoInState.fire(dataEvent<NoEvent, Int>(10))

            assertThat(counter).isEqualTo(15)
        }

        @Test
        fun `adds a do action with parameter`() {
            var counter = 2
            val container = emptyContainer(State(TEST_STATE_1_NAME)).doInState<Int> { data -> counter += data!! }

            container.onDoInState.fire(dataEvent<NoEvent, Int>(19))

            assertThat(counter).isEqualTo(21)
        }

        @Test
        fun `adds a do action`() {
            var counter = 2
            val container = emptyContainer(State(TEST_STATE_1_NAME)).doInState { counter = 42 }

            container.onDoInState.fire(NoEvent)

            assertThat(counter).isEqualTo(42)
        }

        @Test
        fun `adds with chaining`() {
            var entryCounter = 1
            var exitCounter = 1
            val fsm = fsmOf("fsm", State("Start").toContainer())
            val container =
                State(TEST_STATE_1_NAME)
                    .entry<Int> { data -> entryCounter += data!! }
                    .exit<Int> { data -> exitCounter += data!! }
                    .child(fsm)
                    .transition<TestEvent>(State(TEST_STATE_2_NAME))

            container.onEntry.fire(dataEvent<NoEvent, Int>(1))
            container.onExit.fire(dataEvent<NoEvent, Int>(2))

            assertThat(entryCounter).isEqualTo(2)
            assertThat(exitCounter).isEqualTo(3)
            assertThat(container.hasTransitions).isTrue
            assertThat(container.hasChildren).isTrue
        }
    }

    @Nested
    inner class InitialStateContainerTest {
        @Test
        fun `test initialization`() {
            val container = InitialStateContainer(InitialState(), emptyList())

            assertThat(container.name).isEqualTo("Initial")
            assertThat(container.hasTransitions).isFalse
            assertThat(container.hasChildren).isFalse
        }

        @Test
        fun `adds a transition to state`() {
            val container =
                InitialStateContainer(InitialState(), emptyList())
                    .transition(State(TEST_STATE_2_NAME))

            assertThat(container.hasTransitions).isTrue
            assertThat(container.hasChildren).isFalse
        }
    }

    @Nested
    inner class FinalStateContainerTest {
        @Test
        fun `test initialization`() {
            val container = FinalStateContainer(FinalState())

            assertThat(container.name).isEqualTo("Final")
            assertThat(container.hasTransitions).isFalse
            assertThat(container.hasChildren).isFalse
        }
    }

    @Nested
    inner class StateContainerBaseTest {
        @Test
        fun `starting a normal state with parameters`() {
            var counter = 1
            val container =
                spyk(
                    State(TEST_STATE_1_NAME).entry<Int> { data -> counter += data!! },
                    recordPrivateCalls = true,
                )

            container.start(42)

            assertThat(counter).isEqualTo(43)
            verify(exactly = 1) {
                container["startChildren"](any<IEvent>())
            }

            verify(exactly = 0) {
                container["tryStartHistory"](any<IEvent>())
                container["tryStartDeepHistory"]()
            }
        }

        @Test
        fun `starting a normal state without parameters`() {
            var counter = 1
            val container =
                spyk(State(TEST_STATE_1_NAME).entry { counter = 42 }, recordPrivateCalls = true)

            container.start()

            assertThat(counter).isEqualTo(42)
            verify(exactly = 1) {
                container["startChildren"](any<IEvent>())
            }

            verify(exactly = 0) {
                container["tryStartHistory"](any<IEvent>())
                container["tryStartDeepHistory"]()
            }
        }

        @Test
        fun `starting a normal state with history ends in a normal start (calls entry)`() {
            val container = spyk(State(TEST_STATE_1_NAME).toContainer(), recordPrivateCalls = true)

            container.start(NoEvent, History.H)

            verify(exactly = 1) {
                container["tryStartHistory"](any<IEvent>())
                container["startChildren"](any<IEvent>())
            }

            verify(exactly = 0) {
                container["tryStartDeepHistory"]()
            }
        }

        @Test
        fun `starting a parent state with history does not call entry`() {
            val container = spyk(getContainerWithChild(), recordPrivateCalls = true)

            container.start(NoEvent, History.H)

            verify(exactly = 1) {
                container["tryStartHistory"](any<IEvent>())
            }

            verify(exactly = 0) {
                container["tryStartDeepHistory"]()
                container["startChildren"](any<IEvent>())
            }
        }

        @Test
        fun `starting a normal state with deep history ends in a normal start (calls entry)`() {
            val container = spyk(State(TEST_STATE_1_NAME).toContainer(), recordPrivateCalls = true)

            container.start(NoEvent, History.Hd)

            verify(exactly = 1) {
                container["startChildren"](any<IEvent>())
            }
        }

        @Test
        fun `starting a parent state with deep history does not call entry`() {
            val container = spyk(getContainerWithChild(), recordPrivateCalls = true)

            container.start(NoEvent, History.Hd)

            verify(exactly = 1) {
                container["tryStartDeepHistory"]()
            }

            verify(exactly = 0) {
                container["tryStartHistory"](any<IEvent>())
                container["startChildren"](any<IEvent>())
            }
        }

        @Test
        fun `processes transitions`() {
            val container =
                spyk(
                    emptyContainer(State(TEST_STATE_1_NAME))
                        .transition<TestEvent>(State(TEST_STATE_2_NAME))
                        .transition<StopEvent>(State(TEST_STATE_1_NAME)),
                    recordPrivateCalls = true,
                )

            val result = container.trigger(TestEvent)

            assertThat(result.handled).isTrue
            assertThat(result.endPoint?.state?.name).isEqualTo(TEST_STATE_2_NAME)
            verify(exactly = 1) {
                container["processChildren"](TestEvent)
                container["processTransitions"](TestEvent)
            }
        }

        @Test
        fun `processes transitions with unknown event`() {
            val container =
                spyk(
                    emptyContainer(State(TEST_STATE_1_NAME))
                        .transition<StopEvent>(State(TEST_STATE_2_NAME)),
                    recordPrivateCalls = true,
                )

            val result = container.trigger(TestEvent)

            assertThat(result.handled).isFalse
            assertThat(result.endPoint).isNull()
            verify(exactly = 1) {
                container["processChildren"](TestEvent)
                container["processTransitions"](TestEvent)
            }
        }

        @Test
        fun `processes transitions on a child machine`() {
            val container =
                spyk(
                    emptyContainer(State(TEST_STATE_1_NAME))
                        .transition<StopEvent>(State(TEST_STATE_2_NAME)),
                    recordPrivateCalls = true,
                )

            val result = container.trigger(TestEvent)

            assertThat(result.handled).isFalse
            assertThat(result.endPoint).isNull()
            verify(exactly = 1) {
                container["processChildren"](TestEvent)
                container["processTransitions"](TestEvent)
            }
        }

        private fun getComplexContainer(): Pair<StateContainer, StateContainer> {
            val childContainer =
                spyk(
                    State("ChildStart")
                        .transition<TestEvent>(State(TEST_STATE_2_NAME))
                        .transitionToFinal<FinishEvent>(),
                    recordPrivateCalls = true,
                )
            val fsm = spyk(fsmOf("fsmChild", childContainer), recordPrivateCalls = true)
            val container =
                spyk(
                    State(TEST_STATE_1_NAME)
                        .transition<StopEvent>(State(TEST_STATE_2_NAME))
                        .child(fsm),
                    recordPrivateCalls = true,
                )

            return container to childContainer
        }

        @Test
        fun `test trigger child - child handles transition`() {
            val (container, childContainer) = getComplexContainer()
            container.start(NoEvent, History.None)

            val result = container.trigger(TestEvent)

            assertThat(result.handled).isTrue
            assertThat(result.endPoint).isNull()
            verify(exactly = 1) {
                container["processChildren"](TestEvent)
                childContainer["processChildren"](TestEvent)
                childContainer["processTransitions"](TestEvent)
            }
            verify(exactly = 0) {
                container["processTransitions"](TestEvent)
            }
        }

        @Test
        fun `test trigger child - children finish`() {
            val (container, _) = getComplexContainer()
            container.start(NoEvent, History.None)

            val result = container.trigger(FinishEvent)

            assertThat(result.handled).isFalse
            assertThat(result.endPoint).isNull()
        }

        @Test
        fun `test trigger child with data - children finish`() {
            val (container, _) = getComplexContainer()
            container.start(NoEvent, History.None)

            val result = container.trigger(DataEvent(42, FinishEvent::class))

            assertThat(result.handled).isFalse
            assertThat(result.endPoint).isNull()
        }

        @Test
        fun `test trigger child - child does not handle transition`() {
            val (container, childContainer) = getComplexContainer()
            container.start(NoEvent, History.None)

            val result = container.trigger(StopEvent)

            assertThat(result.handled).isTrue
            assertThat(result.endPoint?.state?.name).isEqualTo(TEST_STATE_2_NAME)
            verify(exactly = 1) {
                container["processChildren"](StopEvent)
                container["processTransitions"](StopEvent)
                childContainer["processChildren"](StopEvent)
                childContainer["processTransitions"](StopEvent)
            }
        }
    }

    @Test
    fun `default setting of actions does not throw any exception`() {
        val container = State("Otto").toContainer()

        assertDoesNotThrow { container.onEntry.fire(dataEvent<NoEvent, Int>(1)) }
        assertDoesNotThrow { container.onExit.fire(dataEvent<NoEvent, Int>(3)) }
    }

    companion object {
        private const val TEST_STATE_1_NAME = "test-state-1"
        private const val TEST_STATE_2_NAME = "test-state-2"
        private const val FSM_NAME = "fsm"
    }
}
