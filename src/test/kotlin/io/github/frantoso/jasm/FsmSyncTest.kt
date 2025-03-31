package io.github.frantoso.jasm

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import java.io.InvalidClassException
import java.io.InvalidObjectException
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class FsmSyncTest {
    private object Tick : Event()

    private object Event1 : Event()

    private object Event2 : Event()

    private lateinit var state1: State
    private lateinit var state2: State
    private lateinit var stateContainer1: StateContainer
    private lateinit var stateContainer2: StateContainer
    private lateinit var fsm: FsmSync

    @BeforeEach
    fun createFsm() {
        state1 = State("first")
        state2 = State("second")
        stateContainer1 = state1.transition<Event1>(state2)
        stateContainer2 = state2.transition<Event1>(FinalState())

        fsm =
            FsmSync(
                "fsm",
                { _, _, _ -> },
                { _, _, _, _ -> },
                stateContainer1,
                listOf(stateContainer2),
            )
    }

    private fun createSyncFsm(
        onStateChanged: (sender: Fsm, from: IState, to: IState) -> Unit,
        onTriggered: (sender: Fsm, currentState: IState, event: IEvent, handled: Boolean) -> Unit,
    ): FsmSync {
        val state1 = State("first")
        val state2 = State("second")

        val fsm =
            fsmOf(
                "myFsm",
                onStateChanged,
                onTriggered,
                state1.transition<Event1>(state2).entry<Int> {
                    println(it)
                    Thread.sleep(100)
                },
                state2
                    .transition<Event1>(state2)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    }.transition<Event2>(FinalState()),
            )
        return fsm
    }

    @Test
    fun `creates a new state machine`() {
        val fsm =
            FsmSync(
                "myFsm",
                { _, _, _ -> },
                { _, _, _, _ -> },
                state1.toContainer(),
                emptyList(),
            )
        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState is InitialState).isTrue
        assertThat(fsm.currentState is FinalState).isFalse
        assertThat(fsm.name).isEqualTo("myFsm")
    }

    @Test
    fun `creates a new state machine with onTrigger`() {
        val fsm =
            fsmOf(
                "myFsm",
                { _, _, _ -> },
                state1.toContainer(),
            )
        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState is InitialState).isTrue
        assertThat(fsm.currentState is FinalState).isFalse
        assertThat(fsm.name).isEqualTo("myFsm")
    }

    @Test
    fun `starts the state machine`() {
        assertThat(fsm.isRunning).isFalse

        fsm.start()

        assertThat(fsm.isRunning).isTrue
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState).isSameAs(state1)
        assertThat(fsm.currentState is InitialState).isFalse
        assertThat(fsm.currentState is FinalState).isFalse
    }

    @Test
    fun `trigger changes to the next state`() {
        fsm.start()

        fsm.trigger(Event1)

        assertThat(fsm.isRunning).isTrue
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState).isSameAs(state2)
        assertThat(fsm.currentState is InitialState).isFalse
        assertThat(fsm.currentState is FinalState).isFalse
    }

    @Test
    fun `trigger to final stops the state machine`() {
        fsm.start()
        fsm.trigger(Event1)

        fsm.trigger(Event1)

        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isTrue
        assertThat(fsm.currentState is InitialState).isFalse
        assertThat(fsm.currentState is FinalState).isTrue
    }

    @Test
    fun `using predefined NoEvent on normal transition throws an exception`() {
        fsm.start()

        assertThatThrownBy { fsm.trigger(NoEvent) }.isInstanceOf(FsmException::class.java)
    }

    @Test
    fun `doAction triggers the do action in state (with parameter)`() {
        val state1 = State("first")
        var doInStateResult = 0
        val fsm =
            fsmOf(
                "myFsm",
                state1.doInState<Int> { data -> doInStateResult = data!! },
            )
        fsm.start(42)

        fsm.doAction(22)

        assertThat(doInStateResult).isEqualTo(22)
    }

    @Test
    fun `doAction triggers the do action in state (without parameter)`() {
        val state1 = State("first")
        var doInStateResult = 0
        val fsm =
            fsmOf(
                "myFsm",
                state1.doInState { doInStateResult = 13 },
            )
        fsm.start(42)

        fsm.doAction()

        assertThat(doInStateResult).isEqualTo(13)
    }

    @Test
    fun `adds a destination only state to the states list`() {
        val state1 = State("first")
        val state2 = State("second")
        val state3 = State("third")

        val fsm =
            fsmOf(
                "myFsm",
                state1
                    .transition<Event1>(state2)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    },
                state2
                    .transition<Event1>(state2)
                    .transition<Event2>(state3)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    },
            )

        assertThat(fsm.debugInterface.stateDump.map { it.state }).contains(state3)
    }

    @Test
    fun `adds a destination only state only once to the states list`() {
        val state1 = State("first")
        val state2 = State("second")
        val state3 = State("third")

        val fsm =
            fsmOf(
                "myFsm",
                state1
                    .transition<Event1>(state2)
                    .transition<Event2>(state3)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    },
                state2
                    .transition<Event1>(state2)
                    .transition<Event2>(state3)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    },
            )

        assertThat(
            fsm.debugInterface.stateDump
                .map { it.state }
                .filter { it == state3 },
        ).hasSize(1)
    }

    @Test
    fun `adds more than one destination only state to the states list`() {
        val state1 = State("first")
        val state2 = State("second")
        val state3 = State("third")
        val state4 = State("fourth")

        val fsm =
            fsmOf(
                "myFsm",
                state1
                    .transition<Event1>(state2)
                    .transition<Event2>(state4)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    },
                state2
                    .transition<Event1>(state2)
                    .transition<Event2>(state3)
                    .entry<Int> {
                        println(it)
                        Thread.sleep(100)
                    },
            )

        assertThat(fsm.debugInterface.stateDump.map { it.state }).contains(state3)
        assertThat(fsm.debugInterface.stateDump.map { it.state }).contains(state4)
    }

    @Test
    fun `triggers events synchronously`() {
        val fsm =
            createSyncFsm(
                { machine, from, to -> println("FSM ${machine.name} changed from ${from.name} to ${to.name}") },
                { machine, state, event, handled -> println("$machine - $state - $event - $handled") },
            )

        fsm.start(42)

        runBlocking {
            launch {
                val timeInMillis =
                    measureTimeMillis {
                        while (fsm.isRunning) {
                            delay(10)
                        }
                    }
                println("fsm task (${Thread.currentThread().threadId()}) has run.")

                assertThat(timeInMillis).isGreaterThan(1200)
            }

            launch {
                val timeInMillis =
                    measureTimeMillis {
                        (0..5).forEach {
                            println("triggering $it.")
                            fsm.trigger(Event1, it)
                            delay(10)
                        }
                    }

                fsm.trigger(Event2)
                assertThat(timeInMillis).isGreaterThan(1000)
                println("trigger task 1 (${Thread.currentThread().threadId()}) has run.")
            }

            launch {
                val timeInMillis =
                    measureTimeMillis {
                        (10..15).forEach {
                            println("triggering $it.")
                            fsm.trigger(Event1, it)
                            delay(1)
                        }
                    }

                assertThat(timeInMillis).isGreaterThan(1000)
                println("trigger task 2 (${Thread.currentThread().threadId()}) has run.")
            }
        }

        println("test over")
    }

    @Test
    fun `calling invalid state handler throws FsmException`() {
        val fsm = createSyncFsm({ _, _, _ -> throw InvalidClassException("Test") }, { _, _, _, _ -> })

        assertThrows<FsmException> { fsm.start() }
    }

    @Test
    fun `calling invalid trigger handler throws FsmException`() {
        val fsm = createSyncFsm({ _, _, _ -> }, { _, _, _, _ -> throw InvalidObjectException("Test") })

        assertThrows<FsmException> { fsm.start() }
    }

    @Test
    fun `using NoEvent from a non nested state throws an exception`() {
        val state1 = State("first")
        val state2 = State("second")
        val state3 = State("third")

        assertThrows<FsmException> {
            fsmOf(
                "myFsm",
                state1
                    .transition<Event1>(state2),
                state2
                    .transitionWithoutEvent(state2)
                    .transition<Event2>(state3),
                state3
                    .transition<Event1>(state1),
            )
        }
    }

    @Test
    fun `a call of resume starts the FSM with the specified state`() {
        val state1 = State("first")
        val state2 = State("second")
        val state3 = State("third")

        val fsm =
            fsmOf(
                "myFsm",
                state1
                    .transition<Event1>(state2),
                state2
                    .transition<Event1>(state2)
                    .transition<Event2>(state3),
                state3
                    .transition<Event1>(state1),
            )

        fsm.debugInterface.resume(state2)

        assertThat(fsm.currentState).isEqualTo(state2)
    }

    @Nested
    inner class NestedFsmTests {
        private val state1Child1 = State("state1Child1")
        private val state2Child1 = State("state2Child1")

        private val state1Child2 = State("state1Child2")
        private val state2Child2 = State("state2Child2")
        private val state3Child2 = State("state3Child2")
        private val state4Child2 = State("state4Child2")

        private val state1Main = State("state1Main")
        private val state2Main = State("state2Main")
        private val state3Main = State("state3Main")

        private val childMachine1 =
            fsmOf(
                "child1",
                state1Child1
                    .transition<Tick>(state2Child1),
                state2Child1
                    .transitionToFinal<Tick>(),
            )

        private val childMachine2 =
            fsmOf(
                "child2",
                state1Child2
                    .transition<Tick>(state2Child2),
                state2Child2
                    .transition<Tick>(state3Child2),
                state3Child2
                    .transition<Tick>(state4Child2),
                state4Child2
                    .transitionToFinal<Tick>(),
            )

        private val mainMachine =
            fsmOf(
                "main",
                state1Main
                    .transition<Tick>(state2Main),
                state2Main
                    .children(listOf(childMachine1, childMachine2))
                    .transitionWithoutEvent(state3Main),
                state3Main
                    .transition<Tick>(state1Main),
            )

        @Test
        fun `test use of children`() {
            mainMachine.start()

            assertThat(mainMachine.currentState).isEqualTo(state1Main)
            assertThat(childMachine1.isRunning).isFalse
            assertThat(childMachine2.isRunning).isFalse

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state2Main)
            assertThat(childMachine1.isRunning).isTrue
            assertThat(childMachine2.isRunning).isTrue
            assertThat(childMachine1.currentState).isEqualTo(state1Child1)
            assertThat(childMachine2.currentState).isEqualTo(state1Child2)

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state2Main)
            assertThat(childMachine1.currentState).isEqualTo(state2Child1)
            assertThat(childMachine2.currentState).isEqualTo(state2Child2)

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state2Main)
            assertThat(childMachine1.isRunning).isFalse
            assertThat(childMachine2.currentState).isEqualTo(state3Child2)

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state2Main)
            assertThat(childMachine1.isRunning).isFalse
            assertThat(childMachine2.currentState).isEqualTo(state4Child2)

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state3Main)
            assertThat(childMachine1.isRunning).isFalse
            assertThat(childMachine2.isRunning).isFalse

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state1Main)
            assertThat(childMachine1.isRunning).isFalse
            assertThat(childMachine2.isRunning).isFalse

            mainMachine.trigger(Tick)

            assertThat(mainMachine.currentState).isEqualTo(state2Main)
            assertThat(childMachine1.isRunning).isTrue
            assertThat(childMachine2.isRunning).isTrue
            assertThat(childMachine1.currentState).isEqualTo(state1Child1)
            assertThat(childMachine2.currentState).isEqualTo(state1Child2)
        }
    }
}
