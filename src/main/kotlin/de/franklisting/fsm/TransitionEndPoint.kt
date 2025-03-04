package de.franklisting.fsm

/**
 * A class to store the end point of a transition (state and history).
 * Initializes a new instance of the TransitionEndPoint class.
 *
 * @param T The type of data provided to the condition and action handlers.
 * @param state The destination state of the transition.
 * @param history The type of history to use.
 */
class TransitionEndPoint<T>(
    val state: State<T>,
    val history: History = History.None,
)
