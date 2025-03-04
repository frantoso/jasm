package de.franklisting.fsm

/**
 * A class to store data about a state change.
 * Initializes a new instance of the ChangeStateData class.
 *
 * @param handled A value indicating whether an event was handled.
 * @param endPoint The new end point to change to.
 */
class ChangeStateData<T>(
    val handled: Boolean,
    val endPoint: TransitionEndPoint<T> = TransitionEndPoint(InvalidState<T>()),
)
