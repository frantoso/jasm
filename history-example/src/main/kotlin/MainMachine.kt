package io.github.frantoso

import io.github.frantoso.jasm.Event
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.transition

class MainMachine {
    private val stateFinalizing = State("Finalizing")
    private val stateHandlingError = State("Handling-Error")
    private val stateInitializing = State("Initializing")
    private val stateWorking = Working()

    val machine: Fsm =
        fsmOf(
            "m-machine",
            this.stateInitializing
                .transition<Events.Next>(this.stateWorking),
            this.stateWorking
                .transition<Events.Break>(this.stateHandlingError)
                .transitionWithoutEvent(this.stateFinalizing),
            this.stateHandlingError
                .transition<Events.Restart>(this.stateWorking)
                .transition<Events.Continue>(this.stateWorking.history)
                .transition<Events.ContinueDeep>(this.stateWorking.deepHistory)
                .transition<Events.Next>(this.stateFinalizing)
                .transition<Events.Break>(FinalState()),
            this.stateFinalizing
                .transition<Events.Next>(this.stateInitializing),
        )

    fun start() {
        this.machine.start()
    }

    fun trigger(event: Event) {
        this.machine.trigger(event)
    }
}
