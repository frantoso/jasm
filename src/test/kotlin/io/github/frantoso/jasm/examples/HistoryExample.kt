package io.github.frantoso.jasm.examples

import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.transition
import io.github.frantoso.jasm.transitionToFinal
import io.github.frantoso.jasm.transitionWithoutEvent
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class HistoryExample {
    sealed class Events : Event() {
        object Break : Events()

        object Continue : Events()

        object ContinueDeep : Events()

        object Next : Events()

        object Restart : Events()
    }

    /**
     * The composite state machine of the L2-Preparing state.
     */
    class L2Preparing {
        val stateL3P1 = State("L3-P1")
        val stateL3P2 = State("L3-P2")
        val stateL3P3 = State("L3-P3")
        private val stateL3P4 = State("L3-P4")

        val machine =
            fsmOf(
                "L2-Preparing",
                stateL3P1
                    .transition<Events.Next>(stateL3P2)
                    .entry(::p1Entry),
                stateL3P2
                    .transition<Events.Next>(stateL3P3)
                    .entry(::p2Entry),
                stateL3P3
                    .transition<Events.Next>(stateL3P4)
                    .entry(::p3Entry),
                stateL3P4
                    .transitionToFinal<Events.Next>()
                    .entry(::p4Entry),
            )

        fun p1Entry() = println("L3-P1Entry")

        fun p2Entry() = println("L3-P2Entry")

        fun p3Entry() = println("L3-P3Entry")

        fun p4Entry() = println("L3-P4Entry")
    }

    class L2Working {
        private val stateL3W1 = State("L3-W1")
        private val stateL3W2 = State("L3-W2")
        val stateL3W3 = State("L3-W3")
        val stateL3W4 = State("L3-W4")
        private val stateL3W5 = State("L3-W5")

        val machine =
            fsmOf(
                "L2-Working",
                stateL3W1
                    .transition<Events.Next>(stateL3W2)
                    .entry(::w1Entry),
                stateL3W2
                    .transition<Events.Next>(stateL3W3)
                    .entry(::w2Entry),
                stateL3W3
                    .transition<Events.Next>(stateL3W4)
                    .entry(::w3Entry),
                stateL3W4
                    .transition<Events.Next>(stateL3W5)
                    .entry(::w4Entry),
                stateL3W5
                    .transitionToFinal<Events.Next>()
                    .entry(::w5Entry),
            )

        private fun w5Entry() = println("L3-W5Entry")

        private fun w4Entry() = println("L3-W4Entry")

        private fun w3Entry() = println("L3W-3Entry")

        private fun w2Entry() = println("L3W-2Entry")

        private fun w1Entry() = println("L3W-1Entry")
    }

    class Working {
        private val stateL2Initializing = State("L2-Initializing")
        val stateL2Preparing = State("L2-Preparing")
        val stateL2Working = State("L2-Working")

        val l2Preparing = L2Preparing()

        val l2Working = L2Working()

        val machine =
            fsmOf(
                "Working",
                stateL2Initializing
                    .transition<Events.Next>(stateL2Preparing)
                    .entry(::l2InitializingEntry),
                stateL2Preparing
                    .transitionWithoutEvent(stateL2Working)
                    .entry(::l2PreparingEntry)
                    .child(l2Preparing.machine),
                stateL2Working
                    .transitionToFinal<Events.Next>()
                    .entry(::l2WorkingEntry)
                    .child(l2Working.machine),
            )

        private fun l2InitializingEntry() = println("L2-InitializingEntry")

        private fun l2PreparingEntry() = println("L2-PreparingEntry")

        private fun l2WorkingEntry() = println("L2-WorkingEntry")
    }

    class Main {
        private val stateInitializing = State("Main-Initializing")
        val stateWorking = State("Main-Working")
        val stateHandlingError = State("Main-HandlingError")
        private val stateFinalizing = State("Main-Finalizing")

        val working = Working()

        /**
         * Starts the behavior of the state machine.
         */
        fun start() = machine.start()

        /**
         * Triggers the specified event.
         * @param event The trigger.
         */
        fun trigger(event: Event) = machine.trigger(event)

        val machine =
            fsmOf(
                "Main",
                stateInitializing
                    .transition<Events.Next>(stateWorking),
                stateWorking
                    .transition<Events.Break>(stateHandlingError)
                    .transitionWithoutEvent(stateFinalizing)
                    .entry(::workingEntry)
                    .child(working.machine),
                stateHandlingError
                    .transition<Events.Restart>(stateWorking)
                    .transition<Events.Continue>(stateWorking.history)
                    .transition<Events.ContinueDeep>(stateWorking.deepHistory)
                    .transition<Events.Next>(stateFinalizing)
                    .transitionToFinal<Events.Break>()
                    .entry(::handlingErrorEntry),
                stateFinalizing
                    .transition<Events.Next>(stateInitializing)
                    .entry(::finalizingEntry),
            )

        private fun finalizingEntry() = println("FinalizingEntry")

        private fun handlingErrorEntry() = println("HandlingErrorEntry")

        private fun workingEntry() = println("WorkingEntry")
    }

    @Test
    fun `test history`() {
        val main = Main()
        val working = main.working
        val l2Preparing = working.l2Preparing
        val l2Working = working.l2Working

        main.start()

        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)

        assertThat(main.machine.currentState).isEqualTo(main.stateWorking)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Preparing)
        assertThat(l2Preparing.machine.currentState).isEqualTo(l2Preparing.stateL3P3)
        assertThat(l2Working.machine.isRunning).isFalse

        main.trigger(Events.Break)

        assertThat(main.machine.currentState).isEqualTo(main.stateHandlingError)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Preparing)
        assertThat(l2Preparing.machine.currentState).isEqualTo(l2Preparing.stateL3P3)
        assertThat(l2Working.machine.isRunning).isFalse

        main.trigger(Events.Continue)

        // restore Working, but start L2Preparing from the beginning
        assertThat(main.machine.currentState).isEqualTo(main.stateWorking)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Preparing)
        assertThat(l2Preparing.machine.currentState).isEqualTo(l2Preparing.stateL3P1)
        assertThat(l2Working.machine.isRunning).isFalse

        main.trigger(Events.Next)

        // normal continuation
        assertThat(main.machine.currentState).isEqualTo(main.stateWorking)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Preparing)
        assertThat(l2Preparing.machine.currentState).isEqualTo(l2Preparing.stateL3P2)
        assertThat(l2Working.machine.isRunning).isFalse
    }

    @Test
    fun `test deep history`() {
        val main = Main()
        val working = main.working
        val l2Preparing = working.l2Preparing
        val l2Working = working.l2Working

        main.start()

        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)
        main.trigger(Events.Next)

        assertThat(main.machine.currentState).isEqualTo(main.stateWorking)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Working)
        assertThat(l2Preparing.machine.currentState).isEqualTo(FinalState())
        assertThat(l2Preparing.machine.isRunning).isFalse
        assertThat(l2Working.machine.currentState).isEqualTo(l2Working.stateL3W3)

        main.trigger(Events.Break)

        assertThat(main.machine.currentState).isEqualTo(main.stateHandlingError)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Working)
        assertThat(l2Preparing.machine.currentState).isEqualTo(FinalState())
        assertThat(l2Preparing.machine.isRunning).isFalse
        assertThat(l2Working.machine.currentState).isEqualTo(l2Working.stateL3W3)

        main.trigger(Events.ContinueDeep)

        // restore the states of the whole hierarchy
        assertThat(main.machine.currentState).isEqualTo(main.stateWorking)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Working)
        assertThat(l2Preparing.machine.currentState).isEqualTo(FinalState())
        assertThat(l2Preparing.machine.isRunning).isFalse
        assertThat(l2Working.machine.currentState).isEqualTo(l2Working.stateL3W3)

        main.trigger(Events.Next)

        // normal continuation
        assertThat(main.machine.currentState).isEqualTo(main.stateWorking)
        assertThat(working.machine.currentState).isEqualTo(working.stateL2Working)
        assertThat(l2Preparing.machine.currentState).isEqualTo(FinalState())
        assertThat(l2Preparing.machine.isRunning).isFalse
        assertThat(l2Working.machine.currentState).isEqualTo(l2Working.stateL3W4)
    }

    @Test
    fun `generates a graph from the state machine`() {
        val generator =
            io.github.frantoso.jasm.testutil
                .MultipleDiagramGenerator(Main().machine)

        generator.toSvg("build/reports/history.svg", 1000)
        generator.toPng("build/reports/history.png", 1000)
    }
}
