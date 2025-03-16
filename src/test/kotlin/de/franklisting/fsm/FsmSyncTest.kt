package de.franklisting.fsm

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.io.InvalidClassException
import java.io.InvalidObjectException
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class FsmSyncTest {
    private object Event1 : Event()

    private object Event2 : Event()

    private lateinit var state1: State
    private lateinit var state2: State
    private lateinit var stateContainer1: StateContainer<Int>
    private lateinit var stateContainer2: StateContainer<Int>
    private lateinit var fsm: FsmSync<Int>

    private var doInStateResult = 0

    @BeforeEach
    fun createFsm() {
        state1 = State("first")
        state2 = State("second")
        stateContainer1 = state1.with<Int>().transition(Event1, state2).doInState { i -> doInStateResult = i }
        stateContainer2 = state2.with<Int>().transition(Event1, FinalState())

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
        onStateChanged: (sender: Fsm<Int>, from: IState, to: IState) -> Unit,
        onTriggered: (sender: Fsm<Int>, currentState: IState, event: Event, handled: Boolean) -> Unit,
    ): FsmSync<Int> {
        val state1 = State("first")
        val state2 = State("second")

        val fsm =
            fsmOf(
                "myFsm",
                onStateChanged,
                onTriggered,
                state1.with<Int>().transition(Event1, state2).entry {
                    println(it)
                    Thread.sleep(100)
                },
                state2
                    .with<Int>()
                    .transition(Event1, state2)
                    .entry {
                        println(it)
                        Thread.sleep(100)
                    }.transition(Event2, FinalState()),
            )
        return fsm
    }

    @Test
    fun `creates a new state machine`() {
        val fsm =
            FsmSync<Int>(
                "myFsm",
                { _, _, _ -> },
                { _, _, _, _ -> },
                state1.with(),
                emptyList(),
            )
        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState.state is InitialState).isTrue
        assertThat(fsm.currentState.state is FinalState).isFalse
        assertThat(fsm.name).isEqualTo("myFsm")
    }

    @Test
    fun `starts the state machine`() {
        assertThat(fsm.isRunning).isFalse

        fsm.start(42)

        assertThat(fsm.isRunning).isTrue
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState.state).isSameAs(state1)
        assertThat(fsm.currentState.state is InitialState).isFalse
        assertThat(fsm.currentState.state is FinalState).isFalse
    }

    @Test
    fun `trigger changes to the next state`() {
        fsm.start(42)

        fsm.trigger(Event1, 23)

        assertThat(fsm.isRunning).isTrue
        assertThat(fsm.hasFinished).isFalse
        assertThat(fsm.currentState.state).isSameAs(state2)
        assertThat(fsm.currentState.state is InitialState).isFalse
        assertThat(fsm.currentState.state is FinalState).isFalse
    }

    @Test
    fun `trigger to final stops the state machine`() {
        fsm.start(42)
        fsm.trigger(Event1, 23)

        fsm.trigger(Event1, 23)

        assertThat(fsm.isRunning).isFalse
        assertThat(fsm.hasFinished).isTrue
        assertThat(fsm.currentState.state is InitialState).isFalse
        assertThat(fsm.currentState.state is FinalState).isTrue
    }

    @Test
    fun `using predefined NoEvent on normal transition throws an exception`() {
        fsm.start(42)

        assertThatThrownBy { fsm.trigger(NoEvent, 23) }.isInstanceOf(FsmException::class.java)
    }

    @Test
    fun `doAction triggers the action in state`() {
        doInStateResult = 0
        fsm.start(42)

        fsm.doAction(22)

        assertThat(doInStateResult).isEqualTo(22)
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
                    .with<Int>()
                    .transition(Event1, state2)
                    .entry {
                        println(it)
                        Thread.sleep(100)
                    },
                state2
                    .with<Int>()
                    .transition(Event1, state2)
                    .transition(Event2, state3)
                    .entry {
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
                    .with<Int>()
                    .transition(Event1, state2)
                    .transition(Event2, state3)
                    .entry {
                        println(it)
                        Thread.sleep(100)
                    },
                state2
                    .with<Int>()
                    .transition(Event1, state2)
                    .transition(Event2, state3)
                    .entry {
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
                    .with<Int>()
                    .transition(Event1, state2)
                    .transition(Event2, state4)
                    .entry {
                        println(it)
                        Thread.sleep(100)
                    },
                state2
                    .with<Int>()
                    .transition(Event1, state2)
                    .transition(Event2, state3)
                    .entry {
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

        fsm.start(1)

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

                fsm.trigger(Event2, -1)
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

        assertThrows<FsmException> { fsm.start(1) }
    }

    @Test
    fun `calling invalid trigger handler throws FsmException`() {
        val fsm = createSyncFsm({ _, _, _ -> }, { _, _, _, _ -> throw InvalidObjectException("Test") })

        assertThrows<FsmException> { fsm.start(1) }
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
                    .with<Int>()
                    .transition(Event1, state2),
                state2
                    .with<Int>()
                    .transition(Event1, state2)
                    .transition(Event2, state3),
                state3
                    .with<Int>()
                    .transition(Event1, state1),
            )

        fsm.debugInterface.resume(state2, 3)

        assertThat(fsm.currentState.state).isEqualTo(state2)
    }
}
