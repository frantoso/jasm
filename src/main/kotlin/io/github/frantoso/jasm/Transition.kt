package io.github.frantoso.jasm

/**
 * A class holding all information about a transition.
 * @param T The type of data provided to the condition and action handlers.
 * @param trigger The Event that initiates this transition.
 * @param endPoint A reference to the end point of this transition.
 * @param guard Condition handler of this transition.
 */
data class Transition<T>(
    val trigger: Event,
    val endPoint: TransitionEndPoint,
    val guard: (T) -> Boolean,
) {
    /**
     * Alternative initialization with state as end point.
     * @param trigger The Event that initiates this transition.
     * @param state A reference to the destination state of this transition.
     * @param guard Condition handler of this transition.
     */
    constructor(
        trigger: Event,
        state: EndState,
        guard: (T) -> Boolean,
    ) : this(trigger, TransitionEndPoint(state), guard)

    /**
     * Gets a value indicating whether the end point is the final state.
     */
    val isToFinal = this.endPoint.state is FinalState
}
