package io.github.frantoso.jasm

/**
 * A class to store data about a state change.
 * @param handled A value indicating whether an event was handled.
 * @param endPoint The new end point to change to.
 */
data class ChangeStateData(
    val handled: Boolean,
    val endPoint: TransitionEndPoint? = null,
)
