//package io.github.frantoso.jasm.examples
//
//import io.github.frantoso.jasm.Event
//import io.github.frantoso.jasm.FinalState
//import io.github.frantoso.jasm.NoEvent
//import io.github.frantoso.jasm.State
//import io.github.frantoso.jasm.fsmOf
//import io.github.frantoso.jasm.with
//import org.assertj.core.api.Assertions.assertThat
//import kotlin.test.Test
//
//class HistoryExample {
//    sealed class Events : Event() {
//        object Break : Events()
//
//        object Continue : Events()
//
//        object ContinueDeep : Events()
//
//        object Next : Events()
//
//        object Restart : Events()
//    }
//
//    /**
//     * The composite state machine of the L2-Preparing state.
//     */
//    class L2Preparing {
//        val stateL3P1 = State("L3-P1")
//        val stateL3P2 = State("L3-P2")
//        val stateL3P3 = State("L3-P3")
//        private val stateL3P4 = State("L3-P4")
//
//        val machine =
//            fsmOf(
//                "L2-Preparing",
//                stateL3P1
//                    .with<Any>()
//                    .transition(Events.Next, stateL3P2)
//                    .entry(::p1Entry),
//                stateL3P2
//                    .with<Any>()
//                    .transition(Events.Next, stateL3P3)
//                    .entry(::p2Entry),
//                stateL3P3
//                    .with<Any>()
//                    .transition(Events.Next, stateL3P4)
//                    .entry(::p3Entry),
//                stateL3P4
//                    .with<Any>()
//                    .transitionToFinal(Events.Next)
//                    .entry(::p4Entry),
//            )
//
//        fun p1Entry(data: Any?) = println("P1Entry - $data")
//
//        fun p2Entry(data: Any?) = println("P2Entry - $data")
//
//        fun p3Entry(data: Any?) = println("P3Entry - $data")
//
//        fun p4Entry(data: Any?) = println("P4Entry - $data")
//    }
//
//    class L2Working {
//        private val stateL3W1 = State("L3-W1")
//        private val stateL3W2 = State("L3-W2")
//        val stateL3W3 = State("L3-W3")
//        val stateL3W4 = State("L3-W4")
//        private val stateL3W5 = State("L3-W5")
//
//        val machine =
//            fsmOf(
//                "L2-Working",
//                stateL3W1
//                    .with<Any>()
//                    .transition(Events.Next, stateL3W2)
//                    .entry(::w1Entry),
//                stateL3W2
//                    .with<Any>()
//                    .transition(Events.Next, stateL3W3)
//                    .entry(::w2Entry),
//                stateL3W3
//                    .with<Any>()
//                    .transition(Events.Next, stateL3W4)
//                    .entry(::w3Entry),
//                stateL3W4
//                    .with<Any>()
//                    .transition(Events.Next, stateL3W5)
//                    .entry(::w4Entry),
//                stateL3W5
//                    .with<Any>()
//                    .transitionToFinal(Events.Next)
//                    .entry(::w5Entry),
//            )
//
//        private fun w5Entry(data: Any?) = println("W5Entry - $data")
//
//        private fun w4Entry(data: Any?) = println("W4Entry - $data")
//
//        private fun w3Entry(data: Any?) = println("W3Entry - $data")
//
//        private fun w2Entry(data: Any?) = println("W2Entry - $data")
//
//        private fun w1Entry(data: Any?) = println("W1Entry - $data")
//    }
//
//    class Working {
//        private val stateL2Initializing = State("L2-Initializing")
//        val stateL2Preparing = State("L2-Preparing")
//        val stateL2Working = State("L2-Working")
//
//        val l2Preparing = L2Preparing()
//
//        val l2Working = L2Working()
//
//        val machine =
//            fsmOf(
//                "Working",
//                stateL2Initializing
//                    .with<Any>()
//                    .transition(Events.Next, stateL2Preparing)
//                    .entry(::l2InitializingEntry),
//                stateL2Preparing
//                    .with<Any>()
//                    .transition(NoEvent, stateL2Working)
//                    .entry(::l2PreparingEntry)
//                    .child(l2Preparing.machine),
//                stateL2Working
//                    .with<Any>()
//                    .transitionToFinal(NoEvent)
//                    .entry(::l2WorkingEntry)
//                    .child(l2Working.machine),
//            )
//
//        private fun l2InitializingEntry(data: Any?) = println("L2InitializingEntry - $data")
//
//        private fun l2PreparingEntry(data: Any?) = println("L2PreparingEntry - $data")
//
//        private fun l2WorkingEntry(data: Any?) = println("L2WorkingEntry - $data")
//    }
//
//    class Main {
//        private val stateInitializing = State("Main-Initializing")
//        val stateWorking = State("Main-Working")
//        val stateHandlingError = State("Main-HandlingError")
//        private val stateFinalizing = State("Main-Finalizing")
//
//        val working = Working()
//
//        /**
//         * Starts the behavior of the state machine.
//         */
//        fun start() = machine.start()
//
//        /**
//         * Triggers the specified event.
//         * @param event The trigger.
//         */
//        fun trigger(event: Event) = machine.trigger(event)
//
//        val machine =
//            fsmOf(
//                "Main",
//                stateInitializing
//                    .with<Any>()
//                    .transition(Events.Next, stateWorking),
//                stateWorking
//                    .with<Any>()
//                    .transition(Events.Break, stateHandlingError)
//                    .transition(NoEvent, stateFinalizing)
//                    .entry(::workingEntry)
//                    .child(working.machine),
//                stateHandlingError
//                    .with<Any>()
//                    .transition(Events.Restart, stateWorking)
//                    .transition(Events.Continue, stateWorking.history)
//                    .transition(Events.ContinueDeep, stateWorking.deepHistory)
//                    .transition(Events.Next, stateFinalizing)
//                    .transitionToFinal(Events.Break)
//                    .entry(::handlingErrorEntry),
//                stateFinalizing
//                    .with<Any>()
//                    .transition(Events.Next, stateInitializing)
//                    .entry(::finalizingEntry),
//            )
//
//        private fun finalizingEntry(data: Any?) = println("FinalizingEntry - $data")
//
//        private fun handlingErrorEntry(data: Any?) = println("HandlingErrorEntry - $data")
//
//        private fun workingEntry(data: Any?) = println("WorkingEntry - $data")
//    }
//
//    @Test
//    fun `test history`() {
//        val main = Main()
//        val working = main.working
//        val l2Preparing = working.l2Preparing
//        val l2Working = working.l2Working
//
//        main.start()
//
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateWorking)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Preparing)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(l2Preparing.stateL3P3)
//        assertThat(l2Working.machine.isRunning).isFalse
//
//        main.trigger(Events.Break)
//
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateHandlingError)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Preparing)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(l2Preparing.stateL3P3)
//        assertThat(l2Working.machine.isRunning).isFalse
//
//        main.trigger(Events.Continue)
//
//        // restore Working, but start L2Preparing from the beginning
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateWorking)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Preparing)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(l2Preparing.stateL3P1)
//        assertThat(l2Working.machine.isRunning).isFalse
//
//        main.trigger(Events.Next)
//
//        // normal continuation
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateWorking)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Preparing)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(l2Preparing.stateL3P2)
//        assertThat(l2Working.machine.isRunning).isFalse
//    }
//
//    @Test
//    fun `test deep history`() {
//        val main = Main()
//        val working = main.working
//        val l2Preparing = working.l2Preparing
//        val l2Working = working.l2Working
//
//        main.start()
//
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//        main.trigger(Events.Next)
//
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateWorking)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Working)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(FinalState())
//        assertThat(l2Preparing.machine.isRunning).isFalse
//        assertThat(l2Working.machine.currentState.state).isEqualTo(l2Working.stateL3W3)
//
//        main.trigger(Events.Break)
//
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateHandlingError)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Working)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(FinalState())
//        assertThat(l2Preparing.machine.isRunning).isFalse
//        assertThat(l2Working.machine.currentState.state).isEqualTo(l2Working.stateL3W3)
//
//        main.trigger(Events.ContinueDeep)
//
//        // restore the states of the whole hierarchy
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateWorking)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Working)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(FinalState())
//        assertThat(l2Preparing.machine.isRunning).isFalse
//        assertThat(l2Working.machine.currentState.state).isEqualTo(l2Working.stateL3W3)
//
//        main.trigger(Events.Next)
//
//        // normal continuation
//        assertThat(main.machine.currentState.state).isEqualTo(main.stateWorking)
//        assertThat(working.machine.currentState.state).isEqualTo(working.stateL2Working)
//        assertThat(l2Preparing.machine.currentState.state).isEqualTo(FinalState())
//        assertThat(l2Preparing.machine.isRunning).isFalse
//        assertThat(l2Working.machine.currentState.state).isEqualTo(l2Working.stateL3W4)
//    }
//
//    @Test
//    fun `generates a graph from the state machine`() {
//        val generator =
//            io.github.frantoso.jasm.testutil
//                .MultipleDiagramGenerator(Main().machine)
//
//        generator.toSvg("build/reports/history.svg", 1000)
//        generator.toPng("build/reports/history.png", 1000)
//    }
//}
