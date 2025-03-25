//package io.github.frantoso.jasm
//
//import io.mockk.spyk
//import io.mockk.verify
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.assertDoesNotThrow
//import kotlin.test.Test
//
//class StateContainerTests {
//    private object TestEvent : Event()
//
//    private object StopEvent : Event()
//
//    private fun getContainerWithChild(): StateContainer {
//        val fsm = fsmOf("fsmChild", State("Start").use())
//        val container = State(TEST_STATE_1_NAME).use().child(fsm)
//        container.start()
//        return container
//    }
//
//    private fun emptyContainer(state: State): StateContainer =
//        StateContainer(
//            state,
//            emptyList(),
//            emptyList(),
//            NoAction,
//            NoAction,
//        )
//
//    @Nested
//    inner class StateContainerTest {
//        @Test
//        fun `test initialization`() {
//            val container = emptyContainer(State(TEST_STATE_1_NAME))
//
//            assertThat(container.name).isEqualTo(TEST_STATE_1_NAME)
//            assertThat(container.hasTransitions).isFalse
//            assertThat(container.hasChildren).isFalse
//        }
//
//        @Test
//        fun `adds a child`() {
//            val fsm =
//                fsmOf(
//                    FSM_NAME,
//                    State("Start")
//                        .use(),
//                )
//            val container = emptyContainer(State(TEST_STATE_1_NAME)).child(fsm)
//
//            assertThat(container.hasTransitions).isFalse
//            assertThat(container.hasChildren).isTrue
//            assertThat(container.debugInterface.childDump).hasSize(1)
//        }
//
//        @Test
//        fun `adds a list of children`() {
//            val fsm1 =
//                fsmOf(
//                    FSM_NAME,
//                    State("Start")
//                        .use(),
//                )
//            val fsm2 =
//                fsmOf(
//                    "Otto",
//                    State("Start")
//                        .use(),
//                )
//            val container =
//                emptyContainer(State(TEST_STATE_1_NAME))
//                    .children(listOf(fsm1, fsm2))
//
//            assertThat(container.hasTransitions).isFalse
//            assertThat(container.hasChildren).isTrue
//            assertThat(container.debugInterface.childDump).hasSize(2)
//        }
//
//        @Test
//        fun `adds a transition to state`() {
//            val container =
//                emptyContainer(State(TEST_STATE_1_NAME))
//                    .transition(TestEvent, State(TEST_STATE_2_NAME))
//
//            assertThat(container.hasTransitions).isTrue
//            assertThat(container.hasChildren).isFalse
//        }
//
//        @Test
//        fun `adds a transition to EndPoint`() {
//            val container =
//                emptyContainer(State(TEST_STATE_1_NAME)).transition(
//                    TestEvent,
//                    State(TEST_STATE_2_NAME).deepHistory,
//                )
//
//            assertThat(container.hasTransitions).isTrue
//            assertThat(container.hasChildren).isFalse
//        }
//
//        @Test
//        fun `adds a transition without event to a nested state`() {
//            val fsm = fsmOf("fsm", State("Start").use())
//            val container =
//                emptyContainer(State(TEST_STATE_1_NAME))
//                    .child(fsm)
//                    .transition(State(TEST_STATE_2_NAME).history)
//
//            assertThat(container.hasTransitions).isTrue
//            assertThat(container.hasChildren).isTrue
//            assertThat(
//                container.debugInterface.transitionDump
//                    .first()
//                    .trigger,
//            ).isEqualTo(NoEvent)
//        }
//
//        @Test
//        fun `adds a transition to final state`() {
//            val container = emptyContainer(State(TEST_STATE_1_NAME)).transitionToFinal(TestEvent)
//
//            assertThat(container.hasTransitions).isTrue
//            assertThat(container.hasChildren).isFalse
//        }
//
//        @Test
//        fun `adds an entry action`() {
//            var counter = 5
//            val container = emptyContainer(State(TEST_STATE_1_NAME)).entry { data -> counter += data!! }
//
//            container.fireOnEntry(10)
//
//            assertThat(counter).isEqualTo(15)
//        }
//
//        @Test
//        fun `adds an exit action`() {
//            var counter = 2
//            val container = emptyContainer(State(TEST_STATE_1_NAME)).exit { data -> counter += data!! }
//
//            container.fireOnExit(19)
//
//            assertThat(counter).isEqualTo(21)
//        }
//
//        @Test
//        fun `adds with chaining`() {
//            var entryCounter = 1
//            var exitCounter = 1
//            val fsm = fsmOf("fsm", State("Start").use())
//            val container =
//                State(TEST_STATE_1_NAME)
//                    .use()
//                    .entry { data -> entryCounter += data!! }
//                    .exit { data -> exitCounter += data!! }
//                    .child(fsm)
//                    .transition(TestEvent, State(TEST_STATE_2_NAME))
//
//            container.fireOnEntry(1)
//            container.fireOnExit(2)
//
//            assertThat(entryCounter).isEqualTo(2)
//            assertThat(exitCounter).isEqualTo(3)
//            assertThat(container.hasTransitions).isTrue
//            assertThat(container.hasChildren).isTrue
//        }
//    }
//
//    @Nested
//    inner class InitialStateContainerTest {
//        @Test
//        fun `test initialization`() {
//            val container = InitialStateContainer<Int>(InitialState(), emptyList())
//
//            assertThat(container.name).isEqualTo("Initial")
//            assertThat(container.hasTransitions).isFalse
//            assertThat(container.hasChildren).isFalse
//        }
//
//        @Test
//        fun `adds a transition to state`() {
//            val container =
//                InitialStateContainer<Int>(InitialState(), emptyList())
//                    .transition(State(TEST_STATE_2_NAME))
//
//            assertThat(container.hasTransitions).isTrue
//            assertThat(container.hasChildren).isFalse
//        }
//    }
//
//    @Nested
//    inner class FinalStateContainerTest {
//        @Test
//        fun `test initialization`() {
//            val container = FinalStateContainer<Int>(FinalState())
//
//            assertThat(container.name).isEqualTo("Final")
//            assertThat(container.hasTransitions).isFalse
//            assertThat(container.hasChildren).isFalse
//        }
//    }
//
//    @Nested
//    inner class StateContainerBaseTest {
//        @Test
//        fun `starting a normal state with history ends in a normal start (calls entry)`() {
//            val container = spyk(State(TEST_STATE_1_NAME).use(), recordPrivateCalls = true)
//
//            container.start(NoEvent, History.H)
//
//            verify(exactly = 1) {
//                container["tryStartHistory"](any())
//                container["fireOnEntry"](1)
//                container["startChildren"](1)
//            }
//
//            verify(exactly = 0) {
//                container["tryStartDeepHistory"]()
//            }
//        }
//
//        @Test
//        fun `starting a parent state with history does not call entry`() {
//            val container = spyk(getContainerWithChild(), recordPrivateCalls = true)
//
//            container.start(NoEvent, History.H)
//
//            verify(exactly = 1) {
//                container["tryStartHistory"](1)
//            }
//
//            verify(exactly = 0) {
//                container["tryStartDeepHistory"]()
//                container["fireOnEntry"](any())
//                container["startChildren"](any())
//            }
//        }
//
//        @Test
//        fun `starting a normal state with deep history ends in a normal start (calls entry)`() {
//            val container = spyk(State(TEST_STATE_1_NAME).use(), recordPrivateCalls = true)
//
//            container.start(NoEvent, History.Hd)
//
//            verify(exactly = 1) {
//                container["fireOnEntry"](1)
//                container["startChildren"](1)
//            }
//        }
//
//        @Test
//        fun `starting a parent state with deep history does not call entry`() {
//            val container = spyk(getContainerWithChild(), recordPrivateCalls = true)
//
//            container.start(NoEvent, History.Hd)
//
//            verify(exactly = 1) {
//                container["tryStartDeepHistory"]()
//            }
//
//            verify(exactly = 0) {
//                container["tryStartHistory"](any())
//                container["fireOnEntry"](any())
//                container["startChildren"](any())
//            }
//        }
//
//        @Test
//        fun `processes transitions`() {
//            val container =
//                spyk(
//                    emptyContainer(State(TEST_STATE_1_NAME))
//                        .transition(TestEvent, State(TEST_STATE_2_NAME))
//                        .transition(StopEvent, State(TEST_STATE_1_NAME)),
//                    recordPrivateCalls = true,
//                )
//
//            val result = container.trigger(TestEvent)
//
//            assertThat(result.handled).isTrue
//            assertThat(result.endPoint?.state?.name).isEqualTo(TEST_STATE_2_NAME)
//            verify(exactly = 1) {
//                container["processChildren"](TestEvent, 23)
//                container["processTransitions"](TestEvent, 23)
//            }
//        }
//
//        @Test
//        fun `processes transitions with unknown event`() {
//            val container =
//                spyk(
//                    emptyContainer(State(TEST_STATE_1_NAME))
//                        .transition(StopEvent, State(TEST_STATE_2_NAME)),
//                    recordPrivateCalls = true,
//                )
//
//            val result = container.trigger(TestEvent)
//
//            assertThat(result.handled).isFalse
//            assertThat(result.endPoint).isNull()
//            verify(exactly = 1) {
//                container["processChildren"](TestEvent, 23)
//                container["processTransitions"](TestEvent, 23)
//            }
//        }
//
//        @Test
//        fun `processes transitions on a child machine`() {
//            val container =
//                spyk(
//                    emptyContainer(State(TEST_STATE_1_NAME))
//                        .transition(StopEvent, State(TEST_STATE_2_NAME)),
//                    recordPrivateCalls = true,
//                )
//
//            val result = container.trigger(TestEvent)
//
//            assertThat(result.handled).isFalse
//            assertThat(result.endPoint).isNull()
//            verify(exactly = 1) {
//                container["processChildren"](TestEvent, 23)
//                container["processTransitions"](TestEvent, 23)
//            }
//        }
//
//        private fun getComplexContainer(): Pair<StateContainer<Int>, StateContainer<Int>> {
//            val childContainer =
//                spyk(
//                    State("ChildStart").use().transition(TestEvent, State(TEST_STATE_2_NAME)),
//                    recordPrivateCalls = true,
//                )
//            val fsm = spyk(fsmOf("fsmChild", childContainer), recordPrivateCalls = true)
//            val container =
//                spyk(
//                    State(TEST_STATE_1_NAME)
//                        .use()
//                        .transition(StopEvent, State(TEST_STATE_2_NAME))
//                        .child(fsm),
//                    recordPrivateCalls = true,
//                )
//
//            return container to childContainer
//        }
//
//        @Test
//        fun `test trigger child - child handles transition`() {
//            val (container, childContainer) = getComplexContainer()
//            container.start(NoEvent, History.None)
//
//            val result = container.trigger(TestEvent)
//
//            assertThat(result.handled).isTrue
//            assertThat(result.endPoint).isNull()
//            verify(exactly = 1) {
//                container["processChildren"](TestEvent, 23)
//                childContainer["processChildren"](TestEvent, 23)
//                childContainer["processTransitions"](TestEvent, 23)
//            }
//            verify(exactly = 0) {
//                container["processTransitions"](TestEvent, 23)
//            }
//        }
//
//        @Test
//        fun `test trigger child - child does not handle transition`() {
//            val (container, childContainer) = getComplexContainer()
//            container.start(NoEvent, History.None)
//
//            val result = container.trigger(StopEvent)
//
//            assertThat(result.handled).isTrue
//            assertThat(result.endPoint?.state?.name).isEqualTo(TEST_STATE_2_NAME)
//            verify(exactly = 1) {
//                container["processChildren"](StopEvent, 23)
//                container["processTransitions"](StopEvent, 23)
//                childContainer["processChildren"](StopEvent, 23)
//                childContainer["processTransitions"](StopEvent, 23)
//            }
//        }
//    }
//
//    @Test
//    fun `default setting of actions does not throw any exception`() {
//        val container = State("Otto").use()
//
//        assertDoesNotThrow { container.fireOnEntry(1) }
//        assertDoesNotThrow { container.fireOnExit(3) }
//    }
//
//    companion object {
//        private const val TEST_STATE_1_NAME = "test-state-1"
//        private const val TEST_STATE_2_NAME = "test-state-2"
//        private const val FSM_NAME = "fsm"
//    }
//}
