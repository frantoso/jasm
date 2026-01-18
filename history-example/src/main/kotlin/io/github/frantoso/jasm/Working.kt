package io.github.frantoso.jasm

class Working : CompositeState() {
    private val l2Preparing = L2Preparing()
    private val l2Working = L2Working()
    private val stateInitializing = State("L2-Initializing")

    override val subMachines =
        listOf(
            fsmOf(
                "l2-machine",
                this.stateInitializing
                    .transition<Events.Next>(this.l2Preparing),
                this.l2Preparing
                    .transitionWithoutEvent(this.l2Working),
                this.l2Working
                    .transitionWithoutEvent(FinalState()),
            ),
        )
}
