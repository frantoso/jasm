package de.franklisting.fsm

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
    fun `triggers events asynchronous`() {
        val fsm = FsmAsync<Int>("myFsm")

        val state1 = State<Int>("first")
        val state2 = State<Int>("second")

        fsm.initialTransition(state1)
        state1.transition(Event1, state2).entry {
            println(it)
            Thread.sleep(500)
        }
        state2.transition(Event1, state2).entry {
            println(it)
            Thread.sleep(500)
        }
        state2.transition(Event2, fsm.final)
        fsm.onStateChanged = { machine, from, to -> println("FSM ${machine.name} changed from ${from.name} to ${to.name}") }

        fsm.start(1)

        runBlocking {
            launch {
                val timeInMillis =
                    measureTimeMillis {
                        while (fsm.isRunning) {
                            delay(100)
                        }
                    }
                println("fsm task (${Thread.currentThread().threadId()}) has run.")

                assertThat(timeInMillis).isGreaterThan(6000)
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
