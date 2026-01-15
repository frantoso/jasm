package io.github.frantoso

import io.github.frantoso.jasm.CompositeState
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.State
import io.github.frantoso.jasm.fsmOf
import io.github.frantoso.jasm.transition

class L2Preparing : CompositeState() {
    private val state1 = State("L3p-State 1")
    private val state2 = State("L3p-State 2")
    private val state3 = State("L3p-State 3")
    private val state4 = State("L3p-State 4")

    override val subMachines =
        listOf(
            fsmOf(
                "l3p-machine",
                this.state1
                    .transition<Events.Next>(this.state2),
                this.state2
                    .transition<Events.Next>(this.state3),
                this.state3
                    .transition<Events.Next>(this.state4),
                this.state4
                    .transition<Events.Next>(FinalState()),
            ),
        )
}
