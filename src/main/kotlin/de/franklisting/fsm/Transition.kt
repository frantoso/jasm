package de.franklisting.fsm

/**
 * A class holding all information about a transition.
 *
 * Initializes a new instance of the Transition class.
 *
 * @param T The type of data provided to the condition and action handlers.
 * @param trigger The Event that initiates this transition.
 * @param guard Condition handler of this transition.
 * @param endPoint A reference to the end point of this transition.
 */
data class Transition<T>(
    val trigger: Event,
    val endPoint: TransitionEndPoint<T>,
    val guard: (T) -> Boolean,
) {
    /**
     * Gets a value indicating whether the end point is the final state.
     */
    val isToFinal = this.endPoint.state.isFinal
}
