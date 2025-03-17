package io.github.frantoso.jasm

/**
 * A class to store data about a state change.
 * Initializes a new instance of the ChangeStateData class.
 *
 * @param handled A value indicating whether an event was handled.
 * @param endPoint The new end point to change to.
 */
class ChangeStateData(
    val handled: Boolean,
    val endPoint: TransitionEndPoint? = null,
)
