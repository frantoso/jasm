package io.github.frantoso.jasm

class L2Working : CompositeState() {
    override val subMachines =
        listOf(
            fsmOf(
                "l3w-machine",
                L3WStates.state1
                    .transition<Events.Next>(L3WStates.state2),
                L3WStates.state2
                    .transition<Events.Next>(L3WStates.state3),
                L3WStates.state3
                    .transition<Events.Next>(L3WStates.state4),
                L3WStates.state4
                    .transition<Events.Next>(L3WStates.state5),
                L3WStates.state5
                    .transition<Events.Next>(FinalState()),
            ),
            fsmOf(
                "l3wa-machine",
                L3WaStates.state1
                    .transition<Events.Next>(L3WaStates.state2),
                L3WaStates.state2
                    .transition<Events.Next>(L3WaStates.state3),
                L3WaStates.state3
                    .transition<Events.Next>(FinalState()),
            ),
        )

    private object L3WStates {
        val state1 = State("L3w-State 1")
        val state2 = State("L3w-State 2")
        val state3 = State("L3w-State 3")
        val state4 = State("L3w-State 4")
        val state5 = State("L3w-State 5")
    }

    private object L3WaStates {
        val state1 = State("L3wa-State1")
        val state2 = State("L3wa-State2")
        val state3 = State("L3wa-State3")
    }
}
