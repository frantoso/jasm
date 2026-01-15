package io.github.frantoso.jasm

/**
 * A class to store the end point of a transition (state and history).
 * @param state The destination state of the transition.
 * @param history The type of history to use.
 */
open class TransitionEndPoint(
    val state: EndState,
    val history: History = History.None,
)
