package io.github.frantoso.jasm

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class FsmAsyncTest {
    private object Event1 : Event()

    private object Event2 : Event()

    @Test
    fun `creates a new state machine with onTrigger`() {
        val state1 = State("first")
        val fsm =
            fsmAsyncOf(
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
    fun `triggers events asynchronous`() {
        val state1 = State("first")
        val state2 = State("second")

        val fsm =
            fsmAsyncOf(
                name = "myFsm",
                { machine, from, to -> println("FSM ${machine.name} changed from ${from.name} to ${to.name}") },
                { _, _, _, _ -> },
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

        fsm.start()

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
                            fsm.trigger(Event1)
                            delay(10)
                        }
                    }

                fsm.trigger(Event2)
                assertThat(timeInMillis).isLessThan(500)
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

                assertThat(timeInMillis).isLessThan(500)
                println("trigger task 2 (${Thread.currentThread().threadId()}) has run.")
            }
        }

        println("test over")
    }
}
